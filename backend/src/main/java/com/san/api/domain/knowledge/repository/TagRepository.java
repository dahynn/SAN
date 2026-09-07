package com.san.api.domain.knowledge.repository;

import com.san.api.domain.knowledge.entity.Tag;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

/** 태그 Repository */
public interface TagRepository extends JpaRepository<Tag, UUID> {

    // 태그명 기준 태그 조회
    Optional<Tag> findByTagName(String tagName);
}
