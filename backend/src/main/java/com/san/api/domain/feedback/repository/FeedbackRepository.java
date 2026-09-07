package com.san.api.domain.feedback.repository;

import com.san.api.domain.feedback.entity.Feedback;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

/** 서비스 피드백 엔티티의 영속성 처리를 담당합니다. */
public interface FeedbackRepository extends JpaRepository<Feedback, UUID> {
}
