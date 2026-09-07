package com.san.api.domain.user.repository;

import com.san.api.domain.user.entity.AuthProvider;
import com.san.api.domain.user.entity.User;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;
import java.util.UUID;

/** 서비스 사용자 계정 조회를 담당하는 JPA repository. */
public interface UserRepository extends JpaRepository<User, UUID> {

    Optional<User> findByUsername(String username);

    Optional<User> findByProviderAndProviderId(AuthProvider provider, String providerId);

    boolean existsByUsername(String username);

    /** 사용자 행 잠금 조회 */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("""
            select u
            from User u
            where u.userId = :userId
            """)
    Optional<User> findByUserIdForUpdate(@Param("userId") UUID userId);
}
