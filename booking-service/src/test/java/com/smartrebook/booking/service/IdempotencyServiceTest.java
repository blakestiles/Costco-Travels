package com.smartrebook.booking.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.smartrebook.booking.domain.IdempotencyRecord;
import com.smartrebook.booking.domain.IdempotencyStatus;
import com.smartrebook.booking.dto.ChangeSubmitRequest;
import com.smartrebook.booking.exception.ChangeInProgressException;
import com.smartrebook.booking.exception.IdempotencyConflictException;
import com.smartrebook.booking.repository.IdempotencyRecordRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class IdempotencyServiceTest {

    @Mock
    private IdempotencyRecordRepository repository;

    private ObjectMapper objectMapper;
    private IdempotencyService idempotencyService;

    private static final String KEY = "IDEMP-KEY-1";

    @BeforeEach
    void setUp() {
        objectMapper = new ObjectMapper().registerModule(new JavaTimeModule());
        idempotencyService = new IdempotencyService(repository, objectMapper);
        when(repository.save(org.mockito.ArgumentMatchers.any(IdempotencyRecord.class)))
                .thenAnswer(inv -> inv.getArgument(0));
    }

    private ChangeSubmitRequest payload() {
        return new ChangeSubmitRequest(LocalDate.of(2026, 7, 1), LocalDate.of(2026, 7, 5));
    }

    // 11. Same key + same payload after COMPLETED -> Replay with stored response
    @Test
    void begin_sameKeySamePayloadAfterCompleted_returnsReplayWithStoredResponse() throws Exception {
        when(repository.findByIdempotencyKey(KEY)).thenReturn(Optional.empty());
        IdempotencyService.Outcome first = idempotencyService.begin(KEY, "SUBMIT_CHANGE", payload());
        IdempotencyRecord record = assertInstanceOf(IdempotencyService.New.class, first).record();

        Map<String, Object> responseBody = Map.of("success", true, "newConfirmationNumber", "CT-NEW1");
        idempotencyService.complete(record, responseBody);
        assertEquals(IdempotencyStatus.COMPLETED, record.getStatus());

        when(repository.findByIdempotencyKey(KEY)).thenReturn(Optional.of(record));
        IdempotencyService.Outcome second = idempotencyService.begin(KEY, "SUBMIT_CHANGE", payload());

        IdempotencyService.Replay replay = assertInstanceOf(IdempotencyService.Replay.class, second);
        assertEquals(objectMapper.writeValueAsString(responseBody), replay.responseBodyJson());
    }

    // 12. Same key + different payload -> IdempotencyConflictException
    @Test
    void begin_sameKeyDifferentPayload_throwsIdempotencyConflictException() {
        when(repository.findByIdempotencyKey(KEY)).thenReturn(Optional.empty());
        IdempotencyService.Outcome first = idempotencyService.begin(KEY, "SUBMIT_CHANGE", payload());
        IdempotencyRecord record = assertInstanceOf(IdempotencyService.New.class, first).record();
        idempotencyService.complete(record, Map.of("success", true));

        when(repository.findByIdempotencyKey(KEY)).thenReturn(Optional.of(record));
        ChangeSubmitRequest differentPayload = new ChangeSubmitRequest(LocalDate.of(2026, 8, 1), LocalDate.of(2026, 8, 5));

        assertThrows(IdempotencyConflictException.class,
                () -> idempotencyService.begin(KEY, "SUBMIT_CHANGE", differentPayload));
    }

    // 13. Same key while still IN_PROGRESS -> ChangeInProgressException
    @Test
    void begin_sameKeyStillInProgress_throwsChangeInProgressException() {
        when(repository.findByIdempotencyKey(KEY)).thenReturn(Optional.empty());
        IdempotencyService.Outcome first = idempotencyService.begin(KEY, "SUBMIT_CHANGE", payload());
        IdempotencyRecord record = assertInstanceOf(IdempotencyService.New.class, first).record();
        assertEquals(IdempotencyStatus.IN_PROGRESS, record.getStatus());

        when(repository.findByIdempotencyKey(KEY)).thenReturn(Optional.of(record));

        assertThrows(ChangeInProgressException.class,
                () -> idempotencyService.begin(KEY, "SUBMIT_CHANGE", payload()));
    }
}
