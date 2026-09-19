package com.pawtrail.notification.infrastructure.persistence.jpa;

import com.pawtrail.notification.domain.model.Notification;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

/**
 * 알림 표를 JPA 로 읽고 씁니다.
 *
 * 목록 정렬은 메서드 이름에 적습니다. 최신순이고 같은 시각이면 식별자 순입니다.
 */
public interface NotificationJpaRepository extends JpaRepository<Notification, UUID> {

    Page<Notification> findByAccountIdOrderByCreatedAtDescIdDesc(UUID accountId, Pageable pageable);

    Optional<Notification> findByIdAndAccountId(UUID id, UUID accountId);

    List<Notification> findByAccountIdAndReadAtIsNull(UUID accountId);

    long countByAccountIdAndReadAtIsNull(UUID accountId);
}
