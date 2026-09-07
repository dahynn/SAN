package com.san.api.global.exception.errorcode;

import lombok.AllArgsConstructor;
import lombok.Getter;
import org.springframework.http.HttpStatus;

/**
 * TIL 도메인 에러 코드 (T 계열)
 */
@Getter
@AllArgsConstructor
public enum TilErrorCode implements ErrorCode {

    SUMMARY_NOT_FOUND(HttpStatus.NOT_FOUND, "T001", "존재하지 않는 TIL입니다."),
    SUMMARY_ACCESS_DENIED(HttpStatus.FORBIDDEN, "T002", "해당 TIL에 대한 접근 권한이 없습니다."),
    EMPTY_TIL_SOURCE(HttpStatus.BAD_REQUEST, "T003", "TIL 생성에 사용할 지식 원본이 없습니다."),
    INVALID_TIL_SOURCE_CONTENT(HttpStatus.BAD_REQUEST, "T004", "TIL 생성에 사용할 지식 원본이 유효하지 않습니다."),
    TIL_TITLE_EMPTY(HttpStatus.BAD_REQUEST, "T005", "TIL 제목이 없어 GitHub 커밋을 요청할 수 없습니다."),
    TIL_CONTENT_EMPTY(HttpStatus.BAD_REQUEST, "T006", "TIL 본문이 없어 GitHub 커밋을 요청할 수 없습니다."),
    TIL_ALREADY_COMMITTED(HttpStatus.CONFLICT, "T007", "동일한 TIL 내용이 이미 GitHub 커밋 요청 또는 완료 상태입니다."),
    TIL_GITHUB_REPOSITORY_NOT_CONNECTED(HttpStatus.BAD_REQUEST, "T008", "GitHub 커밋 대상 저장소를 하나만 연결해주세요."),
    TIL_GITHUB_FILE_PATH_UNAVAILABLE(HttpStatus.CONFLICT, "T009", "사용 가능한 TIL GitHub 파일 경로를 찾을 수 없습니다."),
    TIL_GITHUB_COMMIT_NOT_FOUND(HttpStatus.NOT_FOUND, "T010", "존재하지 않는 TIL GitHub 커밋 요청입니다."),
    ;

    private final HttpStatus status;
    private final String code;
    private final String message;
}
