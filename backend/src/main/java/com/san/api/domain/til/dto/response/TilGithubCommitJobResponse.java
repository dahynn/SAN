package com.san.api.domain.til.dto.response;

import com.san.api.domain.til.entity.TilGithubCommitStatus;
import com.san.api.domain.til.service.TilGithubCommitService;

import java.util.UUID;

/** TIL GitHub 커밋 작업 등록 응답 DTO */
public record TilGithubCommitJobResponse(
        UUID commitId,
        UUID jobId,
        UUID summaryId,
        TilGithubCommitStatus status
) {

    /**
     * TIL GitHub 커밋 요청 결과를 응답 DTO로 변환합니다.
     *
     * @param result TIL GitHub 커밋 요청 결과
     * @return TIL GitHub 커밋 작업 등록 응답 DTO
     */
    public static TilGithubCommitJobResponse from(TilGithubCommitService.RequestResult result) {
        return new TilGithubCommitJobResponse(
                result.commitId(),
                result.jobId(),
                result.summaryId(),
                result.status()
        );
    }
}
