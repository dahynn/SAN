package com.san.api.global.entity;

import jakarta.persistence.Column;
import jakarta.persistence.EntityListeners;
import jakarta.persistence.MappedSuperclass;
import lombok.Getter;
import org.hibernate.annotations.SQLRestriction;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDateTime;

/** 모든 도메인 엔티티의 공통 필드(createdAt, updatedAt, isDeleted)를 제공하는 베이스 엔티티. */
@Getter
@MappedSuperclass
@EntityListeners(AuditingEntityListener.class)
@SQLRestriction("is_deleted = false")
public abstract class BaseEntity {

    @CreatedDate
    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @LastModifiedDate
    @Column(nullable = false)
    private LocalDateTime updatedAt;

    @Column(nullable = false)
    private boolean isDeleted = false;

    public void delete() {
        this.isDeleted = true;
    }
}
