package com.san.api.domain.til.entity;

import com.san.api.domain.user.entity.User;
import com.san.api.global.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.Array;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.annotations.SQLRestriction;
import org.hibernate.type.SqlTypes;

import java.time.LocalDate;
import java.util.UUID;

/** 매일의 요약 엔티티 */
@Entity
@Table(name = "daily_summaries")
@SQLRestriction("is_deleted = false")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class DailySummary extends BaseEntity {

    @Id
    @Column(name = "summary_id", columnDefinition = "uuid", updatable = false, nullable = false)
    private UUID summaryId;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(name = "target_date", nullable = false)
    private LocalDate targetDate;

    @Column(name = "title")
    private String title;

    @Column(columnDefinition = "text")
    private String content;

    @JdbcTypeCode(SqlTypes.VECTOR)
    @Array(length = 1536)
    @Column(columnDefinition = "vector(1536)")
    private float[] embedding;

    /**
     * 매일의 요약 생성
     *
     * @param user 요약 소유 사용자
     * @param targetDate 요약 대상 날짜
     * @param content 생성된 TIL 마크다운 내용
     * @param embedding 생성된 TIL 임베딩
     */
    @Builder
    public DailySummary(
            User user,
            LocalDate targetDate,
            String title,
            String content,
            float[] embedding
    ) {
        this.summaryId = UUID.randomUUID();
        this.user = user;
        this.targetDate = targetDate;
        this.title = title;
        this.content = content;
        this.embedding = embedding;
    }

    /**
     * TIL 생성을 위한 빈 매일의 요약 생성
     *
     * @param user 요약 소유 사용자
     * @param targetDate 요약 대상 날짜
     * @return 새 매일의 요약 엔티티
     */
    public static DailySummary create(User user, LocalDate targetDate) {
        return DailySummary.builder()
                .user(user)
                .targetDate(targetDate)
                .build();
    }

    /**
     * AI TIL 생성 결과 갱신
     *
     * @param content 생성된 TIL 마크다운 내용
     * @param embedding 생성된 TIL 임베딩
     */
    public void updateGeneratedResult(String title, String content, float[] embedding) {
        update(title, content, embedding);
    }

    /**
     * TIL 제목과 내용 수정
     *
     * @param title 수정할 TIL 제목
     * @param content 수정할 TIL 내용
     * @param embedding 수정된 TIL 임베딩
     */
    public void update(String title, String content, float[] embedding) {
        this.title = title;
        this.content = content;
        this.embedding = embedding;
    }

    /** TIL을 삭제 처리 */
    public void deleteSummary() {
        delete();
    }

}
