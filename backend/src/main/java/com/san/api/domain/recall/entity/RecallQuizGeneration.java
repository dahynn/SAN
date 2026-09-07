package com.san.api.domain.recall.entity;

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

import java.time.LocalDate;
import java.util.UUID;

/** 리콜 퀴즈 생성 엔티티 */
@Entity
@Table(
        name = "recall_quiz_generations",
        uniqueConstraints = {
                @UniqueConstraint(
                        name = "uk_recall_quiz_generations_user_date_type",
                        columnNames = {"user_id", "target_date", "quiz_type"}
                )
        },
        indexes = {
                @Index(
                        name = "idx_recall_quiz_generations_user_date_type",
                        columnList = "user_id, target_date, quiz_type"
                )
        }
)
@SQLRestriction("is_deleted = false")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class RecallQuizGeneration extends BaseEntity {

    @Id
    @Column(name = "generation_id", columnDefinition = "uuid", updatable = false, nullable = false)
    private UUID generationId;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(name = "target_date", nullable = false)
    private LocalDate targetDate;

    @Enumerated(EnumType.STRING)
    @Column(name = "quiz_type", nullable = false, length = 20)
    private RecallQuizType quizType;

    @Builder
    public RecallQuizGeneration(
            User user,
            LocalDate targetDate,
            RecallQuizType quizType
    ) {
        this.generationId = UUID.randomUUID();
        this.user = user;
        this.targetDate = targetDate;
        this.quizType = quizType;
    }
}
