package com.san.api.domain.knowledge.entity;

import com.san.api.global.entity.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.SQLRestriction;

import java.util.UUID;

/** 태그 엔티티 */
@Entity
@Table(name = "tags")
@SQLRestriction("is_deleted = false")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Tag extends BaseEntity {

    @Id
    @Column(name = "tag_id", columnDefinition = "uuid", updatable = false, nullable = false)
    private UUID tagId;

    @Column(name = "tag_name", nullable = false, unique = true, length = 50)
    private String tagName;

    @Builder
    public Tag(String tagName) {
        this.tagId = UUID.randomUUID();
        this.tagName = tagName;
    }
}
