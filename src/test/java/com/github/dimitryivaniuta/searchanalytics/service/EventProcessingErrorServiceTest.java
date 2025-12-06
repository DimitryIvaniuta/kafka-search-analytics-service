package com.github.dimitryivaniuta.searchanalytics.service;

import com.github.dimitryivaniuta.searchanalytics.model.SearchEventProcessingError;
import com.github.dimitryivaniuta.searchanalytics.repository.SearchEventProcessingErrorRepository;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * Simple unit test verifying that EventProcessingErrorService correctly maps arguments to model.
 */
class EventProcessingErrorServiceTest {

    private final SearchEventProcessingErrorRepository repository =
            mock(SearchEventProcessingErrorRepository.class);

    private final EventProcessingErrorService service =
            new EventProcessingErrorService(repository);

    @Test
    void logError_persistsModelWithExpectedFields() {
        when(repository.save(any(SearchEventProcessingError.class))).thenReturn(42L);

        Long id = service.logError(
                1L,
                "search-events",
                0,
                10L,
                "PROCESSING_ERROR",
                "boom",
                "stack"
        );

        assertThat(id).isEqualTo(42L);
        verify(repository, times(1)).save(any(SearchEventProcessingError.class));
    }
}
