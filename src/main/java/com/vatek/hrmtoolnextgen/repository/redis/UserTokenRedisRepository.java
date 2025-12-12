package com.vatek.hrmtoolnextgen.repository.redis;

import com.vatek.hrmtoolnextgen.entity.redis.UserTokenRedisEntity;
import com.vatek.hrmtoolnextgen.enumeration.EUserTokenType;
import org.springframework.data.repository.CrudRepository;

import java.util.List;

public interface UserTokenRedisRepository extends CrudRepository<UserTokenRedisEntity,String> {
    UserTokenRedisEntity findUserByUserIdAndTokenType(Long userId, EUserTokenType tokenType);
    
    List<UserTokenRedisEntity> findByTokenType(EUserTokenType tokenType);
}
