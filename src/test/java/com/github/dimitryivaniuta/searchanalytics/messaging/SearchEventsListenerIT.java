package com.github.dimitryivaniuta.searchanalytics.messaging;

import com.github.dimitryivaniuta.searchanalytics.config.KafkaTopicsProperties;
import com.github.dimitryivaniuta.searchanalytics.infra.BaseIntegrationTest;
import com.github.dimitryivaniuta.searchanalytics.model.SearchEventPayload;
import com.github.dimitryivaniuta.searchanalytics.repository.DailyQueryStatRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.jdbc.core.JdbcTemplate;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Full integration test:
 *  - send SearchEventPayload to Kafka topic
 *  - listener processes it
 *  - DB tables are updated (daily_query_stats + raw_search_events).
 */
class SearchEventsListenerIT extends BaseIntegrationTest {

    @Autowired
    private KafkaTemplate<String, SearchEventPayload> searchEventKafkaTemplate;

    @Autowired
    private KafkaTopicsProperties topics;

    @Autowired
    private DailyQueryStatRepository dailyQueryStatRepository;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Test
    void searchEvent_isConsumedAndAggregatedAndRawLogged() throws Exception {
        LocalDate today = LocalDate.now(ZoneOffset.UTC);

        SearchEventPayload payload = SearchEventPayload.builder()
                .eventId("evt-1")
                .userId("user-1")
                .query("spring kafka")
                .country("PL")
                .occurredAt(Instant.now())
                .build();

        // send to input topic
        searchEventKafkaTemplate.send(topics.getSearchEventsTopic(), "key-1", payload).get();

        // simple polling loop instead of extra dependency like Awaitility
        boolean statsUpdated = false;
        boolean rawInserted = false;

        for (int i = 0; i < 30; i++) { // up to ~3 seconds
            Thread.sleep(100);

            statsUpdated = dailyQueryStatRepository
                    .findByDayAndQuery(today, "spring kafka")
                    .map(s -> s.getCount() >= 1)
                    .orElse(false);

            Integer rawCount = jdbcTemplate.queryForObject(
                    "SELECT COUNT(*) FROM raw_search_events WHERE user_id = ? AND query = ?",
                    Integer.class,
                    "user-1",
                    "spring kafka"
            );
            rawInserted = rawCount != null && rawCount > 0;

            if (statsUpdated && rawInserted) {
                break;
            }
        }

        assertThat(statsUpdated).as("daily_query_stats updated").isTrue();
        assertThat(rawInserted).as("raw_search_events row inserted").isTrue();
    }
}
