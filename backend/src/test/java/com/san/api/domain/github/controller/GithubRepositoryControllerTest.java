package com.san.api.domain.github.controller;

import com.san.api.domain.github.service.GithubRepositoryService;
import com.san.api.global.exception.BusinessException;
import com.san.api.global.exception.errorcode.CommonErrorCode;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;

class GithubRepositoryControllerTest {

    private final GithubRepositoryController controller =
            new GithubRepositoryController(mock(GithubRepositoryService.class));

    @Test
    void repositoriesRejectsMissingAuthentication() {
        assertThatThrownBy(() -> controller.repositories(null))
                .isInstanceOf(BusinessException.class)
                .extracting("errorCode")
                .isEqualTo(CommonErrorCode.UNAUTHORIZED);
    }
}
