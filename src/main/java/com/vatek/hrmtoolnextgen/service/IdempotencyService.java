package com.vatek.hrmtoolnextgen.service;

import com.vatek.hrmtoolnextgen.entity.redis.IdempotencyKeyRedisEntity;
import com.vatek.hrmtoolnextgen.repository.redis.IdempotencyKeyRedisRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;

import java.util.Optional;

@Service
@RequiredArgsConstructor
@Log4j2
public class IdempotencyService {

    private final IdempotencyKeyRedisRepository idempotencyKeyRedisRepository;

    /**
     * Check if an idempotency key exists and return cached response if found.
     *
     * @param idempotencyKey The idempotency key from the request header
     * @param method         HTTP method
     * @param path           Request path
     * @return Optional containing cached response if found, empty otherwise
     */
    public Optional<ResponseEntity<String>> getCachedResponse(String idempotencyKey, String method, String path) {
        try {
            Optional<IdempotencyKeyRedisEntity> entityOpt = idempotencyKeyRedisRepository.findById(idempotencyKey);

            if (entityOpt.isPresent()) {
                IdempotencyKeyRedisEntity entity = entityOpt.get();
                // Verify that the method and path match
                if (method.equalsIgnoreCase(entity.getMethod()) && path.equals(entity.getPath())) {
                    log.info("Idempotency key found: {} for {} {}", idempotencyKey, method, path);
                    return Optional.of(ResponseEntity
                            .status(HttpStatus.valueOf(entity.getStatusCode()))
                            .body(entity.getResponseBody()));
                } else {
                    log.warn("Idempotency key {} exists but for different endpoint: {} {} vs {} {}",
                            idempotencyKey, entity.getMethod(), entity.getPath(), method, path);
                }
            }
        } catch (Exception e) {
            log.error("Error checking idempotency key: {}", idempotencyKey, e);
        }
        return Optional.empty();
    }

    /**
     * Store the response for an idempotency key.
     *
     * @param idempotencyKey The idempotency key from the request header
     * @param method         HTTP method
     * @param path           Request path
     * @param response       The response to cache
     * @param ttlHours       Time to live in hours
     */
    public void storeResponse(String idempotencyKey, String method, String path,
                              ResponseEntity<String> response, long ttlHours) {
        try {
            String responseBody = response.getBody();
            int statusCode = response.getStatusCode().value();

            IdempotencyKeyRedisEntity entity = IdempotencyKeyRedisEntity.builder()
                    .idempotencyKey(idempotencyKey)
                    .method(method)
                    .path(path)
                    .responseBody(responseBody)
                    .statusCode(statusCode)
                    .ttl(ttlHours)
                    .build();

            idempotencyKeyRedisRepository.save(entity);
            log.info("Stored idempotency key: {} for {} {} with TTL {} hours", idempotencyKey, method, path, ttlHours);
        } catch (Exception e) {
            log.error("Error storing idempotency key: {}", idempotencyKey, e);
        }
    }
}

