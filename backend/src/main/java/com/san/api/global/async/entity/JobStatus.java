package com.san.api.global.async.entity;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum JobStatus {
    PENDING("작업 생성 직후 대기 상태"),
    PROCESSING("비동기 워커가 처리 중인 상태"),
    COMPLETED("AI 응답 수신 및 DB 저장 완료"),
    FAILED("처리 중 오류 발생: error_message 참고");

    private final String description;
}
