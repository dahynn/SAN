package com.san.api.global.async.entity;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum JobType {
    CARD_ANALYSIS("스크랩 원문 기반 지식 카드 AI 분석"),
    SCRAP_REFINE("수집 원본 정제"),
    TIL_GENERATION("지식 카드들을 바탕으로 TIL 문서 생성"),
    TIL_GITHUB_COMMIT("TIL GitHub 커밋 생성"),
    RECALL_QUIZ_GENERATION("리콜 퀴즈 생성"),
    GITHUB_STAR_RECOMMENDATION("GitHub Star 추천 URL 분석");

    private final String description;
}
