package com.san.api.domain.knowledge.entity;

import com.san.api.domain.user.entity.User;
import com.san.api.global.entity.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.SQLRestriction;

import java.util.UUID;

/** 카테고리 엔티티 */
@Entity
@Table(name = "categories")
@SQLRestriction("is_deleted = false")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Category extends BaseEntity {

    @Id
    @Column(name = "category_id", columnDefinition = "uuid", updatable = false, nullable = false)
    private UUID categoryId;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(name = "category_name", nullable = false, length = 100)
    private String categoryName;

    @Builder
    public Category(User user, String categoryName) {
        this.categoryId = UUID.randomUUID();
        this.user = user;
        this.categoryName = categoryName;
    }
}
