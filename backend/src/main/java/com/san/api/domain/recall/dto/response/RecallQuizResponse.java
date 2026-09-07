package com.san.api.domain.recall.dto.response;

import com.san.api.domain.recall.entity.RecallQuiz;
import com.san.api.domain.recall.entity.RecallQuizType;

import java.util.UUID;

/** 리콜 퀴즈 문항 응답 DTO */
public record RecallQuizResponse(
        UUID quizId,
        UUID scrapId,
        RecallQuizType quizType,
        String question,
        boolean solved,
        Boolean correct,
        String submittedAnswer,
        String explanation
) {

    public static RecallQuizResponse from(RecallQuiz quiz) {
        return new RecallQuizResponse(
                quiz.getQuizId(),
                quiz.getScrap().getScrapId(),
                quiz.getQuizType(),
                quiz.getQuestion(),
                quiz.isSolved(),
                quiz.getIsCorrect(),
                quiz.getSubmittedAnswer(),
                quiz.isSolved() ? quiz.getExplanation() : null
        );
    }
}
