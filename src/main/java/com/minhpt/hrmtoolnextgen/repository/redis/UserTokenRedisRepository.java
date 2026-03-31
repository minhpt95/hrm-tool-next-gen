package com.minhpt.hrmtoolnextgen.repository.redis;

import com.minhpt.hrmtoolnextgen.entity.redis.UserTokenRedisEntity;
import com.minhpt.hrmtoolnextgen.enumeration.EUserTokenType;
import org.springframework.data.repository.CrudRepository;

import java.util.List;

public interface UserTokenRedisRepository extends CrudRepository<UserTokenRedisEntity, String> {
    UserTokenRedisEntity findUserByUserIdAndTokenType(Long userId, EUserTokenType tokenType);

    List<UserTokenRedisEntity> findByTokenType(EUserTokenType tokenType);
}
