package com.san.api.global.exception.errorcode;

import lombok.AllArgsConstructor;
import lombok.Getter;
import org.springframework.http.HttpStatus;

/** AI 외부 연동 에러 코드 */
@Getter
@AllArgsConstructor
public enum AiErrorCode implements ErrorCode {

    AI_ANALYSIS_FAILED(HttpStatus.BAD_GATEWAY, "AI001", "AI 분석 요청에 실패했습니다."),
    AI_ANALYSIS_INVALID_RESPONSE(HttpStatus.BAD_GATEWAY, "AI002", "AI 분석 응답이 올바르지 않습니다."),
    AI_TIL_GENERATION_FAILED(HttpStatus.BAD_GATEWAY, "AI003", "AI TIL 생성 요청에 실패했습니다."),
    AI_TIL_INVALID_RESPONSE(HttpStatus.BAD_GATEWAY, "AI004", "AI TIL 생성 응답이 올바르지 않습니다."),
    AI_SCRAP_REFINE_FAILED(HttpStatus.BAD_GATEWAY, "AI005", "AI 원본 정제 요청에 실패했습니다."),
    AI_SCRAP_REFINE_INVALID_RESPONSE(HttpStatus.BAD_GATEWAY, "AI006", "AI 원본 정제 응답이 올바르지 않습니다."),
    AI_QUIZ_GENERATION_FAILED(HttpStatus.BAD_GATEWAY, "AI007", "AI 리콜 퀴즈 생성 요청에 실패했습니다."),
    AI_QUIZ_INVALID_RESPONSE(HttpStatus.BAD_GATEWAY, "AI008", "AI 리콜 퀴즈 생성 응답이 올바르지 않습니다."),
    AI_GITHUB_STAR_RECOMMENDATION_FAILED(HttpStatus.BAD_GATEWAY, "AI009", "GitHub Star 추천 요청에 실패했습니다."),
    AI_GITHUB_STAR_RECOMMENDATION_INVALID_RESPONSE(HttpStatus.BAD_GATEWAY, "AI010", "GitHub Star 추천 응답 형식이 올바르지 않습니다."),
    AI_GITHUB_STAR_RECOMMENDATION_INVALID_REQUEST(HttpStatus.BAD_GATEWAY, "AI011", "GitHub Star 추천 요청 값이 올바르지 않습니다."),
    AI_GITHUB_USER_NOT_FOUND(HttpStatus.NOT_FOUND, "AI012", "GitHub 사용자를 찾을 수 없습니다."),
    AI_GITHUB_STAR_FETCH_FAILED(HttpStatus.BAD_GATEWAY, "AI013", "GitHub Star 목록 조회에 실패했습니다."),
    AI_GITHUB_STAR_SEARCH_FAILED(HttpStatus.BAD_GATEWAY, "AI014", "GitHub Star 추천 검색에 실패했습니다."),
    ;

    private final HttpStatus status;
    private final String code;
    private final String message;
}
