package com.san.api.domain.auth.service;

import com.san.api.domain.auth.dto.response.LoginBridgeTicketResponse;
import com.san.api.domain.auth.entity.ClientType;
import com.san.api.domain.user.entity.UserRole;

/** bridge ticket 발급 결과와 감사 로그에 필요한 컨텍스트를 함께 담습니다. */
record LoginBridgeTicketIssueResult(
        String userId,
        ClientType sourceClientType,
        UserRole role,
        LoginBridgeTicketResponse response
) {
}
