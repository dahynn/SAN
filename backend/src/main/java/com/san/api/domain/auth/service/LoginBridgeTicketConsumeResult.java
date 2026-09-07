package com.san.api.domain.auth.service;

import com.san.api.domain.user.entity.UserRole;

/** bridge ticket 소비 후 토큰 재발급에 필요한 사용자 권한 정보를 담습니다. */
record LoginBridgeTicketConsumeResult(
        String userId,
        UserRole role
) {
}
