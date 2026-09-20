package com.smartrebook.booking.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.smartrebook.booking.domain.IdempotencyRecord;
import com.smartrebook.booking.domain.IdempotencyStatus;
import com.smartrebook.booking.exception.ChangeInProgressException;
import com.smartrebook.booking.exception.IdempotencyConflictException;
import com.smartrebook.booking.repository.IdempotencyRecordRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;
import java.util.Optional;

/**
 * Classic idempotency-key pattern: the same key + same request body always replays the original
 * result without re-calling suppliers; the same key + a different body is a client bug (409).
 * See README: Idempotency.
 */
@Service
public class IdempotencyService {

    private final IdempotencyRecordRepository repository;
    private final ObjectMapper objectMapper;

    public IdempotencyService(IdempotencyRecordRepository repository, ObjectMapper objectMapper) {
        this.repository = repository;
        this.objectMapper = objectMapper;
    }

    public sealed interface Outcome permits New, Replay {
    }

    public record New(IdempotencyRecord record) implements Outcome {
    }

    public record Replay(String responseBodyJson) implements Outcome {
    }

    @Transactional
    public Outcome begin(String idempotencyKey, String operation, Object requestPayload) {
        String hash = hash(requestPayload);
        Optional<IdempotencyRecord> existing = repository.findByIdempotencyKey(idempotencyKey);

        if (existing.isEmpty()) {
            IdempotencyRecord record = new IdempotencyRecord(idempotencyKey, hash, operation, IdempotencyStatus.IN_PROGRESS);
            repository.save(record);
            return new New(record);
        }

        IdempotencyRecord record = existing.get();
        if (!record.getRequestHash().equals(hash)) {
            throw new IdempotencyConflictException(
                    "Idempotency-Key " + idempotencyKey + " was already used with a different request body");
        }
        if (record.getStatus() == IdempotencyStatus.IN_PROGRESS) {
            throw new ChangeInProgressException("This request is already being processed");
        }
        return new Replay(record.getResponseBody());
    }

    @Transactional
    public void complete(IdempotencyRecord record, Object responseBody) {
        record.setStatus(IdempotencyStatus.COMPLETED);
        record.setResponseBody(writeJson(responseBody));
        repository.save(record);
    }

    private String hash(Object payload) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] bytes = digest.digest(writeJson(payload).getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(bytes);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 not available", e);
        }
    }

    private String writeJson(Object value) {
        try {
            return objectMapper.writeValueAsString(value);
        } catch (Exception e) {
            throw new IllegalStateException("Failed to serialize idempotency payload", e);
        }
    }
}
