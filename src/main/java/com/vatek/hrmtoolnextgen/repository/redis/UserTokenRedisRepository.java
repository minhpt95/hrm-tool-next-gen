package com.vatek.hrmtoolnextgen.repository.redis;

import com.vatek.hrmtoolnextgen.entity.redis.UserTokenRedisEntity;
import com.vatek.hrmtoolnextgen.enumeration.EUserTokenType;
import org.springframework.data.repository.CrudRepository;

public interface UserTokenRedisRepository extends CrudRepository<UserTokenRedisEntity,String> {
    UserTokenRedisEntity findUserByUserIdAndTokenType(Long userId, EUserTokenType tokenType);
}
