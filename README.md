# Kafka Search Analytics Service

Spring Boot 3 / Java 21 microservice that consumes all Kafka topic partitions **in a single consumer group instance** to aggregate search events into a PostgreSQL analytics store.

The service demonstrates:

- **Kafka consumer group with 1 consumer** that processes **all partitions** of a topic.
- **At-least-once** processing with manual offset commits.
- **Raw event auditing** in PostgreSQL (`raw_search_events`).
- **Aggregated daily stats** (`daily_query_stats`).
- **Outbox + processing error tables** for real-world robustness.
- **Dead-letter topic** for poison messages.
- Dockerized **Kafka (KRaft)** + **PostgreSQL** + the microservice.

---

## Tech Stack

- **Language:** Java 21
- **Framework:** Spring Boot 3.5.x
- **Build:** Gradle (Groovy DSL) + **version catalog** (`libs.versions.toml`) – no `io.spring.dependency-management`
- **Messaging:** Apache Kafka (KRaft, single node)
- **Database:** PostgreSQL 16
- **Migrations:** Flyway
- **Testing:** JUnit 5, Testcontainers (Kafka + Postgres)
- **Configs:** `application.yml` + `.env` (imported via `spring.config.import`)
- **Containerisation:** Docker & docker-compose

---

## Architecture Overview

### High-Level Flow

1. Frontend / other services send **search events** as JSON to Kafka topic `search-events`.
2. This service (consumer group `search-analytics-cg`) has **one consumer**:
    - Reads from **all partitions** of `search-events`.
    - Logs raw messages into `raw_search_events`.
    - Aggregates counts into `daily_query_stats` (per day + query).
3. On errors:
    - Raw event is marked with `processing_status = ERROR`.
    - A record is stored in `search_event_processing_errors`.
    - A compact message is produced to the **dead-letter topic** `search-events-dlt`.

The consumer uses **manual acknowledgment** and **concurrency = 1**, so each app instance has exactly one consumer that receives all assigned partitions.

---

## Project Layout

```text
kafka-search-analytics-service/
├── README.md
├── build.gradle
├── settings.gradle
├── gradle/
│   └── wrapper/...
├── gradlew
├── gradlew.bat
├── Dockerfile
├── docker-compose.yml
├── .env.example
└── src/
    ├── main/
    │   ├── java/com/github/dimitryivaniuta/searchanalytics/
    │   │   ├── SearchAnalyticsApplication.java
    │   │   ├── config/
    │   │   │   ├── KafkaConfig.java
    │   │   │   └── KafkaTopicsProperties.java
    │   │   ├── messaging/
    │   │   │   ├── SearchEventsListener.java
    │   │   │   └── DeadLetterProducer.java
    │   │   ├── model/
    │   │   │   ├── SearchEventPayload.java
    │   │   │   ├── DailyQueryStat.java
    │   │   │   ├── RawSearchEvent.java
    │   │   │   ├── SearchEventOutbox.java
    │   │   │   └── SearchEventProcessingError.java
    │   │   ├── repository/
    │   │   │   ├── DailyQueryStatRepository.java
    │   │   │   ├── RawSearchEventRepository.java
    │   │   │   ├── SearchEventOutboxRepository.java
    │   │   │   └── SearchEventProcessingErrorRepository.java
    │   │   ├── service/
    │   │   │   ├── DailyQueryStatService.java
    │   │   │   ├── RawSearchEventService.java
    │   │   │   ├── SearchEventOutboxService.java
    │   │   │   ├── EventProcessingErrorService.java
    │   │   │   └── SearchEventProducerService.java
    │   │   └── web/
    │   │       ├── HealthController.java
    │   │       ├── StatsQueryController.java
    │   │       └── SearchEventController.java
    │   └── resources/
    │       ├── application.yml
    │       ├── db/migration/
    │       │   ├── V1__init_daily_query_stats.sql
    │       │   ├── V2__raw_search_events.sql
    │       │   ├── V3__search_event_outbox.sql
    │       │   └── V4__search_event_processing_errors.sql
    │       ├── logback-spring.xml
    │       └── banner.txt (optional)
    └── test/
        ├── java/com/github/dimitryivaniuta/searchanalytics/
        │   ├── infra/BaseIntegrationTest.java
        │   ├── repository/DailyQueryStatRepositoryIT.java
        │   ├── messaging/SearchEventsListenerIT.java
        │   └── service/
        │       ├── DailyQueryStatServiceTest.java
        │       └── EventProcessingErrorServiceTest.java
        └── resources/
            ├── application-test.yml
            └── logback-test.xml
```

---

## Configuration

### 1. `.env`

Copy `.env.example` to `.env` in project root and adjust if needed:

```dotenv
SPRING_PROFILES_ACTIVE=local

SERVER_PORT=8080

# Kafka exposed on host as localhost:9095
KAFKA_BOOTSTRAP_SERVERS=localhost:9095
KAFKA_CONSUMER_GROUP_ID=search-analytics-cg

SEARCH_EVENTS_TOPIC=search-events
SEARCH_EVENTS_DLT_TOPIC=search-events-dlt
SEARCH_OUTBOX_TOPIC=search-events-outbox

SPRING_DATASOURCE_URL=jdbc:postgresql://postgres:5432/search_analytics
SPRING_DATASOURCE_USERNAME=search_analytics
SPRING_DATASOURCE_PASSWORD=search_analytics
```

`application.yml` imports this file via:

```yaml
spring:
  config:
    import: "optional:file:.env[.properties]"
```

### 2. Kafka & Postgres via docker-compose

Key parts of `docker-compose.yml`:

- **Kafka (KRaft):**
    - Internal: `kafka:29092`
    - External (host): **`localhost:9095`**
    - `KAFKA_ADVERTISED_LISTENERS=INTERNAL://kafka:29092,EXTERNAL://localhost:9095`

- **Postgres:**
    - `localhost:5432`
    - DB: `search_analytics`
    - User: `search_analytics` / `search_analytics`

---

## Database Schema

Managed by Flyway migrations under `src/main/resources/db/migration`.

### `daily_query_stats`

Aggregated stats per day + query:

```sql
CREATE TABLE daily_query_stats (
    id    BIGSERIAL PRIMARY KEY,
    day   DATE     NOT NULL,
    query TEXT     NOT NULL,
    count BIGINT   NOT NULL DEFAULT 0,
    CONSTRAINT uq_daily_query_stats_day_query UNIQUE (day, query)
);
```

### `raw_search_events`

Audit table containing every consumed Kafka message:

- Unique constraint on `(kafka_topic, kafka_partition, kafka_offset)` for idempotency.
- `payload` stored as `JSONB`.
- `processing_status` (RECEIVED | PROCESSED | SKIPPED | ERROR).

### `search_event_outbox`

Transactional outbox table for downstream events (optional).

### `search_event_processing_errors`

Detailed error log linked to `raw_search_events`.

---

## Kafka Consumer Strategy

- **Group id:** `search-analytics-cg`
- **Topic:** `search-events`
- **Listener:** `SearchEventsListener`
- **Concurrency:** `1`
- **Ack mode:** `MANUAL`

This guarantees:

- One consumer instance per app instance.
- That instance receives **all partitions** assigned to the group.
- Offsets are committed only after successful processing.

Error handling:

- Deserialization configured via `JsonDeserializer` (or ErrorHandlingDeserializer).
- Processing failures result in:
    - `raw_search_events.processing_status = ERROR`
    - Row in `search_event_processing_errors`
    - Message written to `search-events-dlt` via `DeadLetterProducer`

---

## REST API

### 1. Health

**GET** `/api/health`

```json
{
  "status": "UP",
  "timestamp": "2025-12-07T19:31:12.356+01:00"
}
```

**GET** `/api/health/ready` – simple readiness probe including configured Kafka topics.

---

### 2. Publish search events (for Postman / manual testing)

**POST** `/api/search-events`

Body (example):

```json
{
  "userId": "user-123",
  "query": "spring kafka consumer group",
  "country": "PL",
  "locale": "pl-PL",
  "deviceType": "desktop",
  "platform": "web",
  "source": "search-bar",
  "filters": {
    "category": "programming",
    "subCategory": "java",
    "page": 0,
    "pageSize": 20,
    "sortBy": "relevance",
    "sortDirection": "desc"
  }
}
```

The server will:

- Fill `eventId`, `occurredAt`, `sentAt` if missing.
- Use `userId` / `anonymousId` / `sessionId` as Kafka key.
- Produce the message to topic `search-events`.

Response:

```json
{
  "status": "ACCEPTED",
  "eventId": "20f19f19-07b8-4b6c-9bb1-74d3e5fdf9d1",
  "occurredAt": "2025-12-07T19:30:00Z",
  "sentAt": "2025-12-07T19:31:12.345Z",
  "timestamp": "2025-12-07T19:31:12.356+01:00"
}
```

---

### 3. Query aggregated stats

**GET** `/api/stats/daily?day=2025-12-07&limit=10`

Returns top queries for a given day:

```json
[
  { "id": 1, "day": "2025-12-07", "query": "spring kafka", "count": 12 },
  { "id": 2, "day": "2025-12-07", "query": "java 21", "count": 5 }
]
```

**GET** `/api/stats/range?from=2025-12-01&to=2025-12-07&limit=10`

Aggregated across day range (no `id`/`day` – only `query` and total `count`).

---

## Running the Project

### 1. Start infrastructure

```bash
docker-compose up -d
```

Verify services:

- Kafka: `localhost:9095`
- Postgres: `localhost:5432`

### 2. Build & run the app (host)

```bash
./gradlew clean bootJar
java -jar build/libs/kafka-search-analytics-service-*.jar
```

The app reads `.env` and `application.yml` for configuration.

### 3. Run via Docker

```bash
docker-compose up -d --build
```

This builds the service image and runs:

- Kafka
- Postgres
- Search Analytics Service (port 8080 on host)

---

## Testing

### Unit tests

```bash
./gradlew test
```

### Integration tests (Testcontainers)

The integration tests (`BaseIntegrationTest` and friends) spin up:

- A temporary Postgres container.
- A temporary Kafka container.

They verify:

- `daily_query_stats` repository behavior (upsert, aggregation).
- Full flow Kafka → `SearchEventsListener` → DB.

### Manual testing via Postman

1. Start `docker-compose` + app.
2. POST `/api/search-events` with JSON payload.
3. GET `/api/stats/daily` to see aggregated results.
4. Inspect `raw_search_events` and `daily_query_stats` tables in Postgres.

---

## Kafka CLI Examples

From **host**:

```bash
# List topics
kafka-topics --bootstrap-server localhost:9095 --list

# Describe consumer group
kafka-consumer-groups --bootstrap-server localhost:9095   --describe --group search-analytics-cg

# Tail messages from DLT
kafka-console-consumer --bootstrap-server localhost:9095   --topic search-events-dlt   --from-beginning
```

---

## Notes & Extensions

- For production, you’d typically:
    - Secure Kafka (SASL/SSL).
    - Add metrics/observability (Micrometer / OpenTelemetry).
    - Implement a scheduled **outbox publisher** that reads from `search_event_outbox` and publishes downstream events.
- The schema and service code are intentionally simple but **ready to be extended** with:
    - More dimensions (device, locale, filters).
    - Time-windowed aggregations.
    - Multiple consumer groups (e.g. one for analytics, one for ML features).

## License

This project is licensed under the [MIT License](LICENSE).

---

## Contact

**Dzmitry Ivaniuta** — [diafter@gmail.com](mailto:diafter@gmail.com) — [GitHub](https://github.com/DimitryIvaniuta)
