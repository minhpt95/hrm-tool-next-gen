package com.vatek.hrmtoolnextgen.entity.redis;

import com.vatek.hrmtoolnextgen.enumeration.EUserTokenType;
import lombok.*;
import org.springframework.data.annotation.Id;
import org.springframework.data.redis.core.RedisHash;
import org.springframework.data.redis.core.TimeToLive;
import org.springframework.data.redis.core.index.Indexed;

import java.util.concurrent.TimeUnit;

@RedisHash(value = "user_token")
@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class UserTokenRedisEntity {
    @Id
    @Indexed
    private Long userId;

    @Indexed
    private EUserTokenType tokenType;

    private String token;

    @TimeToLive(unit = TimeUnit.MILLISECONDS)
    private Long ttl;
}
