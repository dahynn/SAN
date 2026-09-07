package com.san.api.domain.recall.service;

import com.san.api.domain.recall.dto.request.RecallQuizSubmitRequest;
import com.san.api.domain.recall.dto.response.RecallQuizSubmitResponse;
import com.san.api.domain.recall.entity.RecallQuiz;
import com.san.api.domain.recall.entity.RecallQuizType;
import com.san.api.domain.recall.repository.RecallQuizRepository;
import com.san.api.global.exception.BusinessException;
import com.san.api.global.exception.errorcode.RecallErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Locale;
import java.util.UUID;

/** 리콜 퀴즈 정답 제출 Service */
@Service
@RequiredArgsConstructor
public class RecallQuizSubmissionService {

    private static final String O_ANSWER = "O";
    private static final String X_ANSWER = "X";

    private final RecallQuizRepository recallQuizRepository;

    /**
     * 리콜 퀴즈 정답 제출
     *
     * @param userId 사용자 ID
     * @param quizId 리콜 퀴즈 ID
     * @param request 리콜 퀴즈 정답 제출 요청
     * @return 리콜 퀴즈 정답 제출 응답
     */
    @Transactional
    public RecallQuizSubmitResponse submit(UUID userId, UUID quizId, RecallQuizSubmitRequest request) {
        RecallQuiz quiz = recallQuizRepository.findByQuizIdAndUser_UserId(quizId, userId)
                .orElseThrow(() -> new BusinessException(RecallErrorCode.RECALL_QUIZ_NOT_FOUND));

        String answer = normalizeAnswer(request.answer());
        if (quiz.getQuizType() == RecallQuizType.OX) {
            submitOxAnswer(quiz, answer);
        } else {
            quiz.submitShortAnswer(answer, LocalDateTime.now());
        }

        return RecallQuizSubmitResponse.from(quiz);
    }

    /** OX 답변 검증 후 채점 */
    private void submitOxAnswer(RecallQuiz quiz, String answer) {
        String oxAnswer = answer.toUpperCase(Locale.ROOT);
        if (!O_ANSWER.equals(oxAnswer) && !X_ANSWER.equals(oxAnswer)) {
            throw new BusinessException(RecallErrorCode.INVALID_RECALL_QUIZ_ANSWER);
        }

        quiz.submitOxAnswer(oxAnswer, LocalDateTime.now());
    }

    /** 제출 답변 공백 제거 */
    private String normalizeAnswer(String answer) {
        if (answer == null || answer.trim().isEmpty()) {
            throw new BusinessException(RecallErrorCode.INVALID_RECALL_QUIZ_ANSWER);
        }

        return answer.trim();
    }
}
