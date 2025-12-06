# Kafka CLI cheat-sheet for **kafka-search-analytics-service**

This project uses **Confluent Kafka 8.x** in Docker.

- Docker service / container name: **kafkasas**
- Broker internal listener (inside container): **localhost:9092**
- Broker from host (mapped port): **localhost:9095**
- Main topic: **`search-events`**
- Consumer group used by this app: **`search-analytics-cg`**

> ℹ️ Use the commands **inside the Kafka container** unless explicitly stated otherwise.

---

## 1. Open a shell in the Kafka container

From your host:

```bash
# Docker Desktop / docker-compose v2
docker compose exec kafkasas bash

# or generic Docker
docker exec -it kafkasas bash
```

You should see a prompt similar to:

```text
sh-5.1$
```

All commands below assume you are at that prompt.

---

## 2. Topic management

### 2.1 List topics

```bash
kafka-topics --bootstrap-server localhost:9092 --list
```

### 2.2 Create topic `search-events` (if not already created)

```bash
kafka-topics   --bootstrap-server localhost:9092   --create   --topic search-events   --partitions 4   --replication-factor 1
```

### 2.3 Describe the topic

```bash
kafka-topics   --bootstrap-server localhost:9092   --describe   --topic search-events
```

---

## 3. Produce test events for Search Analytics

The search-analytics service listens on topic **`search-events`** and expects
JSON matching `SearchEventPayload`.

### 3.1 Start console producer with keys

```bash
kafka-console-producer   --bootstrap-server localhost:9092   --topic search-events   --property parse.key=true   --property key.separator=:
```

Now each line you type is:

```text
<key>:<json-payload>
```

### 3.2 Example payloads (copy/paste lines)

> 💡 Adjust `occurredAt` to **today’s date** if you want to see results for `day=TODAY` in `/api/stats/daily`.

```text
user-1:{"eventId":"evt-1","userId":"user-1","query":"spring kafka","country":"PL","occurredAt":"2025-12-06T10:00:00Z","sentAt":"2025-12-06T10:00:01Z"}
user-2:{"eventId":"evt-2","userId":"user-2","query":"java streams","country":"DE","occurredAt":"2025-12-06T11:15:00Z","sentAt":"2025-12-06T11:15:02Z"}
user-1:{"eventId":"evt-3","userId":"user-1","query":"spring kafka","country":"PL","occurredAt":"2025-12-06T12:00:00Z","sentAt":"2025-12-06T12:00:01Z"}
```

- **Key** (`user-1`, `user-2`) is used for partitioning.
- Required fields for aggregation:
  - `query`
  - `occurredAt` (ISO UTC timestamp)

Press `Ctrl + C` to stop the producer.

The service will:

1. Insert raw copies into `raw_search_events`.
2. Update counters in `daily_query_stats`.

---

## 4. Inspect consumer groups

The service uses consumer group **`search-analytics-cg`**.

### 4.1 List all groups

```bash
kafka-consumer-groups   --bootstrap-server localhost:9092   --list
```

You should now see:

```text
search-analytics-cg
```

(if the application is running and connected to Kafka).

### 4.2 Describe the group

```bash
kafka-consumer-groups   --bootstrap-server localhost:9092   --describe   --group search-analytics-cg
```

This shows:

- topic/partition assignment
- current offsets
- lag

---

## 5. Query aggregated stats from the service (Postman / curl)

With the Spring Boot app running on **host** (port `8080` by default):

### 5.1 Daily stats

```bash
curl "http://localhost:8080/api/stats/daily?day=2025-12-06&limit=10"
```

Expected response shape:

```json
[
  { "id": 1, "day": "2025-12-06", "query": "spring kafka", "count": 2 },
  { "id": 2, "day": "2025-12-06", "query": "java streams", "count": 1 }
]
```

### 5.2 Range stats

```bash
curl "http://localhost:8080/api/stats/range?from=2025-12-01&to=2025-12-31&limit=10"
```

This aggregates counts per query across the date range (day field is `null` in the response because it’s aggregated).

---

## 6. Quick troubleshooting checklist

- **No stats appear in DB / `/stats`**:
  - Check consumer group exists:

    ```bash
    kafka-consumer-groups --bootstrap-server localhost:9092 --list
    ```

  - If `search-analytics-cg` is missing, verify app config:

    - `.env` → `KAFKA_BOOTSTRAP_SERVERS=localhost:9095`
    - `application.yml` → `spring.kafka.bootstrap-servers=${KAFKA_BOOTSTRAP_SERVERS:localhost:9095}`

- **Producer works but app logs show connection errors**:
  - Confirm Kafka service is up (`kafkasas` container running).
  - Confirm port mapping: `9095:9092` in `docker-compose.yml`.

---
