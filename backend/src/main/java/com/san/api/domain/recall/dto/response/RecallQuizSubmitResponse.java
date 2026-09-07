package com.san.api.domain.recall.dto.response;

import com.san.api.domain.recall.entity.RecallQuiz;
import com.san.api.domain.recall.entity.RecallQuizType;

import java.util.UUID;

/** 리콜 퀴즈 정답 제출 응답 DTO */
public record RecallQuizSubmitResponse(
        UUID quizId,
        RecallQuizType quizType,
        String question,
        boolean solved,
        Boolean correct,
        String submittedAnswer,
        String explanation
) {

    public static RecallQuizSubmitResponse from(RecallQuiz quiz) {
        return new RecallQuizSubmitResponse(
                quiz.getQuizId(),
                quiz.getQuizType(),
                quiz.getQuestion(),
                quiz.isSolved(),
                quiz.getIsCorrect(),
                quiz.getSubmittedAnswer(),
                quiz.getExplanation()
        );
    }
}
