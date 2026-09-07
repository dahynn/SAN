package com.san.api.domain.recall.service;

import com.san.api.domain.recall.dto.request.RecallQuizSubmitRequest;
import com.san.api.domain.recall.dto.response.RecallQuizSubmitResponse;
import com.san.api.domain.recall.entity.RecallQuiz;
import com.san.api.domain.recall.entity.RecallQuizType;
import com.san.api.domain.recall.repository.RecallQuizRepository;
import com.san.api.domain.scrap.entity.Scrap;
import com.san.api.domain.scrap.entity.SourceType;
import com.san.api.domain.til.entity.DailySummary;
import com.san.api.domain.user.entity.AuthProvider;
import com.san.api.domain.user.entity.User;
import com.san.api.global.exception.BusinessException;
import com.san.api.global.exception.errorcode.RecallErrorCode;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDate;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class RecallQuizSubmissionServiceTest {

    @Mock
    private RecallQuizRepository recallQuizRepository;

    @InjectMocks
    private RecallQuizSubmissionService recallQuizSubmissionService;

    private UUID userId;
    private User user;
    private DailySummary summary;
    private Scrap scrap;

    @BeforeEach
    void setUp() {
        userId = UUID.randomUUID();
        user = buildUser(userId);
        summary = buildSummary();
        scrap = buildScrap();
    }

    @Test
    void submitShortAnswerSavesSubmittedAnswerWithoutScoring() {
        RecallQuiz quiz = buildQuiz(RecallQuizType.SHORT_ANSWER, "정답");

        when(recallQuizRepository.findByQuizIdAndUser_UserId(quiz.getQuizId(), userId))
                .thenReturn(Optional.of(quiz));

        RecallQuizSubmitResponse response = recallQuizSubmissionService.submit(
                userId,
                quiz.getQuizId(),
                new RecallQuizSubmitRequest("  사용자 답변  ")
        );

        assertThat(response.solved()).isTrue();
        assertThat(response.correct()).isNull();
        assertThat(response.submittedAnswer()).isEqualTo("사용자 답변");
        assertThat(response.explanation()).isEqualTo("해설");
        assertThat(quiz.getSubmittedAnswer()).isEqualTo("사용자 답변");
        assertThat(quiz.getIsCorrect()).isNull();
        assertThat(quiz.getSolvedAt()).isNotNull();
    }

    @Test
    void submitOxAnswerNormalizesAndScoresAnswer() {
        RecallQuiz quiz = buildQuiz(RecallQuizType.OX, "O");

        when(recallQuizRepository.findByQuizIdAndUser_UserId(quiz.getQuizId(), userId))
                .thenReturn(Optional.of(quiz));

        RecallQuizSubmitResponse response = recallQuizSubmissionService.submit(
                userId,
                quiz.getQuizId(),
                new RecallQuizSubmitRequest("  o  ")
        );

        assertThat(response.solved()).isTrue();
        assertThat(response.correct()).isTrue();
        assertThat(response.submittedAnswer()).isEqualTo("O");
        assertThat(quiz.getSubmittedAnswer()).isEqualTo("O");
        assertThat(quiz.getIsCorrect()).isTrue();
        assertThat(quiz.getSolvedAt()).isNotNull();
    }

    @Test
    void submitOxAnswerThrowsExceptionWhenAnswerIsInvalid() {
        RecallQuiz quiz = buildQuiz(RecallQuizType.OX, "O");

        when(recallQuizRepository.findByQuizIdAndUser_UserId(quiz.getQuizId(), userId))
                .thenReturn(Optional.of(quiz));

        assertThatThrownBy(() -> recallQuizSubmissionService.submit(
                userId,
                quiz.getQuizId(),
                new RecallQuizSubmitRequest("A")
        ))
                .isInstanceOf(BusinessException.class)
                .hasFieldOrPropertyWithValue("errorCode", RecallErrorCode.INVALID_RECALL_QUIZ_ANSWER);
    }

    @Test
    void submitThrowsExceptionWhenQuizIsNotFound() {
        UUID quizId = UUID.randomUUID();

        when(recallQuizRepository.findByQuizIdAndUser_UserId(quizId, userId))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() -> recallQuizSubmissionService.submit(
                userId,
                quizId,
                new RecallQuizSubmitRequest("O")
        ))
                .isInstanceOf(BusinessException.class)
                .hasFieldOrPropertyWithValue("errorCode", RecallErrorCode.RECALL_QUIZ_NOT_FOUND);
    }

    private RecallQuiz buildQuiz(RecallQuizType quizType, String answer) {
        return RecallQuiz.builder()
                .dailySummary(summary)
                .scrap(scrap)
                .quizType(quizType)
                .question("질문")
                .answer(answer)
                .explanation("해설")
                .build();
    }

    private DailySummary buildSummary() {
        return DailySummary.builder()
                .user(user)
                .targetDate(LocalDate.of(2026, 5, 19))
                .title("TIL title")
                .content("TIL content")
                .build();
    }

    private Scrap buildScrap() {
        return Scrap.builder()
                .user(user)
                .sourceType(SourceType.TEXT)
                .rawContent("raw content")
                .build();
    }

    private User buildUser(UUID userId) {
        User user = User.builder()
                .username("testuser")
                .provider(AuthProvider.LOCAL)
                .build();
        ReflectionTestUtils.setField(user, "userId", userId);
        return user;
    }
}
