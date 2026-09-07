package com.san.api.domain.recall.entity;

import com.san.api.domain.scrap.entity.Scrap;
import com.san.api.domain.til.entity.DailySummary;
import com.san.api.domain.user.entity.User;
import com.san.api.global.entity.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.SQLRestriction;

import java.time.LocalDateTime;
import java.util.Objects;
import java.util.UUID;

/** 리콜 퀴즈 엔티티 */
@Entity
@Table(
        name = "recall_quizzes",
        indexes = {
                @Index(
                        name = "idx_recall_quizzes_user_summary_type",
                        columnList = "user_id, summary_id, quiz_type"
                )
        },
        uniqueConstraints = {
                @UniqueConstraint(
                        name = "uk_recall_quizzes_summary_scrap_type",
                        columnNames = {"summary_id", "scrap_id", "quiz_type"}
                )
        }
)
@SQLRestriction("is_deleted = false")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class RecallQuiz extends BaseEntity {

    @Id
    @Column(name = "quiz_id", columnDefinition = "uuid", updatable = false, nullable = false)
    private UUID quizId;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "summary_id", nullable = false)
    private DailySummary dailySummary;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "scrap_id", nullable = false)
    private Scrap scrap;

    @Enumerated(EnumType.STRING)
    @Column(name = "quiz_type", nullable = false, length = 20)
    private RecallQuizType quizType;

    @Column(nullable = false, columnDefinition = "text")
    private String question;

    @Column(nullable = false, length = 255)
    private String answer;

    @Column(columnDefinition = "text")
    private String explanation;

    @Column(name = "submitted_answer", length = 255)
    private String submittedAnswer;

    @Column(name = "is_solved", nullable = false)
    private boolean isSolved = false;

    @Column(name = "is_correct")
    private Boolean isCorrect;

    @Column(name = "solved_at")
    private LocalDateTime solvedAt;

    @Builder
    public RecallQuiz(
            DailySummary dailySummary,
            Scrap scrap,
            RecallQuizType quizType,
            String question,
            String answer,
            String explanation
    ) {
        validateSameOwner(dailySummary, scrap);

        this.quizId = UUID.randomUUID();
        this.user = dailySummary.getUser();
        this.dailySummary = dailySummary;
        this.scrap = scrap;
        this.quizType = quizType;
        this.question = question;
        this.answer = answer;
        this.explanation = explanation;
    }

    /**
     * 단답형 답변 제출 상태 저장
     *
     * @param submittedAnswer 사용자가 제출한 단답형 답변
     * @param solvedAt 풀이 시각
     */
    public void submitShortAnswer(String submittedAnswer, LocalDateTime solvedAt) {
        this.submittedAnswer = submittedAnswer;
        this.isSolved = true;
        this.isCorrect = null;
        this.solvedAt = solvedAt;
    }

    /**
     * OX 답변 제출 및 채점 결과 저장
     *
     * @param submittedAnswer 사용자가 제출한 OX 답변
     * @param solvedAt 풀이 시각
     */
    public void submitOxAnswer(String submittedAnswer, LocalDateTime solvedAt) {
        this.submittedAnswer = submittedAnswer;
        this.isSolved = true;
        this.isCorrect = answer.equals(submittedAnswer);
        this.solvedAt = solvedAt;
    }

    /** 소유자 일치 검증 */
    private void validateSameOwner(DailySummary dailySummary, Scrap scrap) {
        Objects.requireNonNull(dailySummary, "dailySummary must not be null");
        Objects.requireNonNull(scrap, "scrap must not be null");

        if (!Objects.equals(dailySummary.getUser().getUserId(), scrap.getUser().getUserId())) {
            throw new IllegalArgumentException("Daily summary and scrap owner must be same.");
        }
    }
}
