package com.san.api.domain.feedback.entity;

import com.san.api.domain.auth.entity.ClientType;
import com.san.api.domain.user.entity.User;
import com.san.api.global.entity.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.UUID;

/**
 * 사용자가 서비스 안에서 남긴 피드백과 요청 추적용 메타데이터를 저장하는 엔티티입니다.
 *
 * 사용자가 입력한 내용뿐 아니라 요청 컨텍스트의 traceId, IP, User-Agent를 함께 저장하여
 * 릴리즈 이후 이슈 재현과 로그 추적에 활용합니다.
 */
@Getter
@Entity
@Table(name = "feedback")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Feedback extends BaseEntity {

    @Id
    @Column(name = "feedback_id", columnDefinition = "uuid", updatable = false, nullable = false)
    private UUID feedbackId;

    /** 피드백을 등록한 사용자. */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id")
    private User user;

    /** 사용자가 선택한 피드백 유형. */
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private FeedbackType type;

    /** 사용자가 작성한 피드백 본문. */
    @Column(nullable = false, columnDefinition = "text")
    private String content;

    /** 답변이 필요한 경우 사용자가 남긴 연락처. */
    @Column(name = "contact", length = 255)
    private String contact;

    /** 피드백을 작성한 프론트엔드 화면 URL. */
    @Column(name = "page_url", length = 1000)
    private String pageUrl;

    /** 피드백을 보낸 클라이언트 유형. */
    @Enumerated(EnumType.STRING)
    @Column(name = "client_type", length = 20)
    private ClientType clientType;

    /** 요청 로그와 연결하기 위한 trace id. */
    @Column(name = "trace_id", length = 100)
    private String traceId;

    /** 요청을 보낸 클라이언트 IP. */
    @Column(name = "ip_address", length = 45)
    private String ipAddress;

    /** 요청을 보낸 브라우저 또는 확장 프로그램 User-Agent. */
    @Column(name = "user_agent", columnDefinition = "text")
    private String userAgent;

    /** 피드백 처리 상태. */
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private FeedbackStatus status;

    @Builder
    private Feedback(
            User user,
            FeedbackType type,
            String content,
            String contact,
            String pageUrl,
            ClientType clientType,
            String traceId,
            String ipAddress,
            String userAgent
    ) {
        this.feedbackId = UUID.randomUUID();
        this.user = user;
        this.type = type;
        this.content = content;
        this.contact = contact;
        this.pageUrl = pageUrl;
        this.clientType = clientType;
        this.traceId = traceId;
        this.ipAddress = ipAddress;
        this.userAgent = userAgent;
        this.status = FeedbackStatus.NEW;
    }
}
