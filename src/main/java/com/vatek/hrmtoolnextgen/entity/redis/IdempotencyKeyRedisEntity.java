package com.vatek.hrmtoolnextgen.entity.redis;

import lombok.*;
import org.springframework.data.annotation.Id;
import org.springframework.data.redis.core.RedisHash;
import org.springframework.data.redis.core.TimeToLive;
import org.springframework.data.redis.core.index.Indexed;

import java.util.concurrent.TimeUnit;

@RedisHash(value = "idempotency_key")
@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class IdempotencyKeyRedisEntity {
    @Id
    @Indexed
    private String idempotencyKey;

    @Indexed
    private String method;

    @Indexed
    private String path;

    private String responseBody;

    private Integer statusCode;

    @TimeToLive(unit = TimeUnit.HOURS)
    private Long ttl;
}

