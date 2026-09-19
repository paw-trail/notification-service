package com.pawtrail.notification.infrastructure.persistence.jpa;

import com.pawtrail.notification.domain.enums.NotifType;
import com.pawtrail.notification.domain.model.Notification;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

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

    /**
     * 그 사람들의 그 장소 · 그 종류 알림 가운데 안 읽은 것을 한 문장으로 지웁니다.
     *
     * 지우기 전에 밀려 있던 쓰기를 먼저 반영합니다(flushAutomatically).
     * 같은 트랜잭션에서 앞서 만든 알림이 아직 DB 에 안 들어갔다면 이 문장이 그것을 못 보기 때문입니다.
     */
    @Modifying(flushAutomatically = true)
    @Query("""
            delete from Notification n
            where n.accountId in :accountIds
              and n.placeId = :placeId
              and n.notifType = :notifType
              and n.readAt is null
            """)
    int deleteUnread(@Param("accountIds") Collection<UUID> accountIds,
                     @Param("placeId") UUID placeId,
                     @Param("notifType") NotifType notifType);
}
