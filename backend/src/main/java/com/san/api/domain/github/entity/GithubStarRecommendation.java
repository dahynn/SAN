package com.san.api.domain.github.entity;

import com.san.api.domain.user.entity.User;
import com.san.api.global.entity.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.UUID;

/** GitHub Star 추천 후보 엔티티 */
@Entity
@Table(
        name = "github_star_recommendations",
        indexes = {
                @Index(name = "idx_github_star_recommendations_user_id", columnList = "user_id"),
                @Index(name = "idx_github_star_recommendations_job_id", columnList = "job_id")
        }
)
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class GithubStarRecommendation extends BaseEntity {

    @Id
    @Column(name = "github_star_recommendation_id", columnDefinition = "uuid", updatable = false, nullable = false)
    private UUID githubStarRecommendationId;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(name = "job_id", columnDefinition = "uuid", nullable = false)
    private UUID jobId;

    @Column(nullable = false, columnDefinition = "text")
    private String url;

    @Column(nullable = false)
    private String title;

    @Column(nullable = false, columnDefinition = "text")
    private String summary;

    @Column(name = "analysis_result", nullable = false, columnDefinition = "text")
    private String analysisResult;

    @Column(nullable = false)
    private boolean collected;

    @Column(name = "collected_target_id", columnDefinition = "uuid")
    private UUID collectedTargetId;

    public GithubStarRecommendation(
            User user,
            UUID jobId,
            String url,
            String title,
            String summary,
            String analysisResult
    ) {
        this.githubStarRecommendationId = UUID.randomUUID();
        this.user = user;
        this.jobId = jobId;
        this.url = url;
        this.title = title;
        this.summary = summary;
        this.analysisResult = analysisResult;
        this.collected = false;
    }

    /** 추천 후보 수집 완료 처리 */
    public void markCollected(UUID collectedTargetId) {
        this.collected = true;
        this.collectedTargetId = collectedTargetId;
    }
}
