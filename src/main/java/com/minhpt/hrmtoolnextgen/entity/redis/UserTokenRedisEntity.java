package com.minhpt.hrmtoolnextgen.entity.redis;

import com.minhpt.hrmtoolnextgen.enumeration.EUserTokenType;
import lombok.*;
import org.springframework.data.annotation.Id;
import org.springframework.data.redis.core.RedisHash;
import org.springframework.data.redis.core.TimeToLive;
import org.springframework.data.redis.core.index.Indexed;

import java.util.concurrent.TimeUnit;

@RedisHash(value = "user_tokens")
@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class UserTokenRedisEntity {
    @Id
    private String id; // Composite key: userId:tokenType

    @Indexed
    private Long userId;

    @Indexed
    private EUserTokenType tokenType;

    private String token;

    @TimeToLive(unit = TimeUnit.MILLISECONDS)
    private Long ttl;
}
