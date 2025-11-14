package com.vatek.hrmtoolnextgen.repository.redis;

import com.vatek.hrmtoolnextgen.entity.redis.IdempotencyKeyRedisEntity;
import org.springframework.data.repository.CrudRepository;

public interface IdempotencyKeyRedisRepository extends CrudRepository<IdempotencyKeyRedisEntity, String> {
}

