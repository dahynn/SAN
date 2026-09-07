package com.san.api.domain.github.controller;

import com.san.api.domain.github.service.GithubLinkService;
import com.san.api.global.exception.BusinessException;
import com.san.api.global.exception.errorcode.CommonErrorCode;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;

class GithubLinkControllerTest {

    private final GithubLinkController controller = new GithubLinkController(mock(GithubLinkService.class));

    @Test
    void statusRejectsMissingAuthentication() {
        assertThatThrownBy(() -> controller.status(null))
                .isInstanceOf(BusinessException.class)
                .extracting("errorCode")
                .isEqualTo(CommonErrorCode.UNAUTHORIZED);
    }

    @Test
    void authorizeUrlRejectsMissingAuthentication() {
        assertThatThrownBy(() -> controller.authorizeUrl(null))
                .isInstanceOf(BusinessException.class)
                .extracting("errorCode")
                .isEqualTo(CommonErrorCode.UNAUTHORIZED);
    }
}
