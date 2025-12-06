package com.github.dimitryivaniuta.searchanalytics.service;

import com.github.dimitryivaniuta.searchanalytics.model.SearchEventPayload;
import com.github.dimitryivaniuta.searchanalytics.repository.DailyQueryStatRepository;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

/**
 * Unit tests for DailyQueryStatService – verifies date conversion & delegation.
 */
class DailyQueryStatServiceTest {

    private final DailyQueryStatRepository repository = mock(DailyQueryStatRepository.class);
    private final DailyQueryStatService service = new DailyQueryStatService(repository);

    @Test
    void incrementFromEvent_convertsOccurredAtToUtcDay() {
        Instant occurredAt = Instant.parse("2025-12-06T10:15:30Z");
        SearchEventPayload payload = SearchEventPayload.builder()
                .query("java streams")
                .occurredAt(occurredAt)
                .build();

        service.incrementFromEvent(payload);

        ArgumentCaptor<LocalDate> dayCaptor = ArgumentCaptor.forClass(LocalDate.class);
        ArgumentCaptor<String> queryCaptor = ArgumentCaptor.forClass(String.class);

        verify(repository, times(1)).incrementCount(dayCaptor.capture(), queryCaptor.capture());

        assertThat(dayCaptor.getValue()).isEqualTo(occurredAt.atZone(ZoneOffset.UTC).toLocalDate());
        assertThat(queryCaptor.getValue()).isEqualTo("java streams");
    }
}
