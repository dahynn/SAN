package com.san.api.domain.auth.service;

import com.san.api.domain.auth.entity.ClientType;
import com.san.api.global.security.redis.AuthRedisKeyPrefix;
import org.springframework.stereotype.Component;

/** 인증 세션별 Redis key 생성을 담당합니다. */
@Component
public class AuthSessionKeyService {

    /** refresh token을 사용자, 클라이언트, 세션 단위로 분리해 저장하기 위한 key를 만듭니다. */
    public String refreshKey(String userId, ClientType clientType, String sessionId) {
        return AuthRedisKeyPrefix.REFRESH + userId + ":" + clientType.name() + ":" + sessionId;
    }

    public String userRefreshIndexKey(String userId) {
        return AuthRedisKeyPrefix.REFRESH + "index:user:" + userId;
    }
}
