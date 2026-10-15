package com.shop.order.adapter.in.web;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataAccessException;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;

import java.net.URI;
import java.time.Duration;
import java.util.function.Supplier;

/**
 * Idempotency tầng API bằng Redis (Idempotency-Key):
 *   1. SET NX key=IN_PROGRESS (TTL ngắn)  -> chỉ 1 request được chạy
 *   2. Chạy xong -> ghi COMPLETED + response (TTL 24h)
 *   3. Request trùng: cùng nội dung -> trả lại response cũ; khác nội dung -> 422; đang chạy -> 409
 * Redis chỉ là lớp tối ưu: nếu Redis lỗi/mất dữ liệu thì chạy tiếp (fail-open) và
 * unique constraint (customer_id, idempotency_key) trong Postgres vẫn đảm bảo không tạo 2 đơn.
 */
@Component
class IdempotencyExecutor {

    private static final Logger log = LoggerFactory.getLogger(IdempotencyExecutor.class);

    private static final String PREFIX = "idem:order:";
    private static final Duration IN_PROGRESS_TTL = Duration.ofSeconds(60);
    private static final Duration RESULT_TTL = Duration.ofHours(24);
    private static final int MAX_KEY_LENGTH = 128;

    static class InvalidKeyException extends RuntimeException {
        InvalidKeyException(String message) { super(message); }
    }

    static class KeyReuseException extends RuntimeException {
        KeyReuseException() { super("Idempotency-Key was already used with a different request payload"); }
    }

    static class RequestInProgressException extends RuntimeException {
        RequestInProgressException() { super("A request with this Idempotency-Key is still being processed"); }
    }

    record Stored(String state, String fingerprint, int status, String location, String body) {
        boolean inProgress() { return "IN_PROGRESS".equals(state); }
    }

    private final StringRedisTemplate redis;
    private final ObjectMapper mapper;

    IdempotencyExecutor(StringRedisTemplate redis, ObjectMapper mapper) {
        this.redis = redis;
        this.mapper = mapper;
    }

    static String requireValidKey(String key) {
        if (key == null || key.isBlank()) {
            throw new InvalidKeyException("Header 'Idempotency-Key' is required");
        }
        if (key.length() > MAX_KEY_LENGTH) {
            throw new InvalidKeyException("Header 'Idempotency-Key' must be at most " + MAX_KEY_LENGTH + " characters");
        }
        return key.trim();
    }

    ResponseEntity<?> execute(String scope, String key, String fingerprint, Supplier<ResponseEntity<?>> action) {
        String redisKey = PREFIX + scope + ":" + key;

        for (int attempt = 0; attempt < 3; attempt++) {
            boolean acquired;
            try {
                acquired = Boolean.TRUE.equals(redis.opsForValue().setIfAbsent(
                        redisKey, write(new Stored("IN_PROGRESS", fingerprint, 0, null, null)), IN_PROGRESS_TTL));
            } catch (DataAccessException redisDown) {
                log.warn("Redis unavailable for idempotency check, relying on DB constraint: {}", redisDown.toString());
                return action.get();
            }

            if (acquired) {
                return runAndStore(redisKey, fingerprint, action);
            }

            Stored existing = read(redis.opsForValue().get(redisKey));
            if (existing == null) {
                continue; // key vừa hết hạn giữa 2 lệnh -> thử giành lại
            }
            if (!existing.fingerprint().equals(fingerprint)) {
                throw new KeyReuseException();
            }
            if (existing.inProgress()) {
                throw new RequestInProgressException();
            }
            return replay(existing);
        }
        throw new RequestInProgressException();
    }

    private ResponseEntity<?> runAndStore(String redisKey, String fingerprint, Supplier<ResponseEntity<?>> action) {
        ResponseEntity<?> response;
        try {
            response = action.get();
        } catch (RuntimeException ex) {
            safeDelete(redisKey); // lỗi -> cho phép client retry với cùng key
            throw ex;
        }
        try {
            URI location = response.getHeaders().getLocation();
            Stored done = new Stored("COMPLETED", fingerprint, response.getStatusCode().value(),
                    location == null ? null : location.toString(), mapper.writeValueAsString(response.getBody()));
            redis.opsForValue().set(redisKey, write(done), RESULT_TTL);
        } catch (JsonProcessingException | DataAccessException ex) {
            // Đơn đã tạo xong; nếu không lưu được cache thì request lặp sẽ được DB trả lại đơn cũ
            log.warn("Could not cache idempotent response: {}", ex.toString());
        }
        return response;
    }

    private ResponseEntity<?> replay(Stored stored) {
        ResponseEntity.BodyBuilder builder = ResponseEntity.status(stored.status())
                .header("Idempotent-Replayed", "true")
                .contentType(MediaType.APPLICATION_JSON);
        if (stored.location() != null) {
            builder.location(URI.create(stored.location()));
        }
        return builder.body(stored.body()); // body là JSON thô đã lưu
    }

    private void safeDelete(String redisKey) {
        try {
            redis.delete(redisKey);
        } catch (DataAccessException ex) {
            log.warn("Could not release idempotency key: {}", ex.toString());
        }
    }

    private String write(Stored stored) {
        try {
            return mapper.writeValueAsString(stored);
        } catch (JsonProcessingException e) {
            throw new IllegalStateException(e);
        }
    }

    private Stored read(String json) {
        if (json == null) {
            return null;
        }
        try {
            return mapper.readValue(json, Stored.class);
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("Corrupted idempotency record", e);
        }
    }
}
