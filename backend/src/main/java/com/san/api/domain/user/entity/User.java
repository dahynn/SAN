package com.san.api.domain.user.entity;

import com.san.api.global.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * 회원 엔티티.
 *
 * PK는 UUID(version 4)를 사용하여 순차 노출로 인한 정보 유출을 방지합니다.
 * 탈퇴는 논리 삭제(status = WITHDRAWN)로 처리하여 감사 추적을 보존합니다.
 */
@Entity
@Table(
        name = "users",
        indexes = {
                @Index(name = "idx_users_username", columnList = "username"),
                @Index(name = "idx_users_provider_provider_id", columnList = "provider, provider_id")
        }
)
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class User extends BaseEntity {

    @Id
    @Column(name = "user_id", columnDefinition = "uuid", updatable = false, nullable = false)
    private UUID userId;

    @Column(unique = true, length = 30)
    private String username;

    /** 소셜 로그인 계정은 null 허용. */
    @Column(name = "password_hash")
    private String passwordHash;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 10)
    private AuthProvider provider;

    @Column(name = "provider_id")
    private String providerId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 10)
    private UserStatus status;

    @Enumerated(EnumType.STRING)
    @Column(length = 20)
    private UserRole role;

    /** 계정 잠금 해제 시각. LOCKED 상태일 때만 유효. */
    @Column(name = "locked_until")
    private LocalDateTime lockedUntil;

    @Builder
    private User(String username, String passwordHash, AuthProvider provider, String providerId) {
        this.userId = UUID.randomUUID();
        this.username = username;
        this.passwordHash = passwordHash;
        this.provider = provider;
        this.providerId = providerId;
        this.status = UserStatus.ACTIVE;
        this.role = UserRole.USER;
    }

    // ────────────────────────────── 도메인 행위 ──────────────────────────────

    /** 로그인 실패 한도 초과 시 계정을 일시 잠금합니다. */
    public void lock(LocalDateTime until) {
        this.status = UserStatus.LOCKED;
        this.lockedUntil = until;
    }

    /** 잠금 기간이 지났으면 자동 해제합니다. */
    public void unlockIfExpired() {
        if (this.status == UserStatus.LOCKED
                && this.lockedUntil != null
                && LocalDateTime.now().isAfter(this.lockedUntil)) {
            this.status = UserStatus.ACTIVE;
            this.lockedUntil = null;
        }
    }

    /** 회원 탈퇴 (논리 삭제). */
    public void withdraw() {
        this.status = UserStatus.WITHDRAWN;
    }

    public boolean isActive() {
        return this.status == UserStatus.ACTIVE;
    }

    public boolean isLocked() {
        return this.status == UserStatus.LOCKED;
    }

    public boolean isWithdrawn() {
        return this.status == UserStatus.WITHDRAWN;
    }

    public boolean isAdmin() {
        return this.role == UserRole.ADMIN;
    }
}
