package com.pawtrail.notification.infrastructure.persistence;

import com.pawtrail.notification.domain.enums.NotifType;
import com.pawtrail.notification.domain.model.Notification;
import com.pawtrail.notification.domain.repository.NotificationRepository;
import com.pawtrail.notification.infrastructure.persistence.jpa.NotificationJpaRepository;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Repository;

/**
 * 도메인이 선언한 알림 저장소 약속을 JPA 로 구현합니다.
 */
@Repository
@RequiredArgsConstructor
public class NotificationRepositoryImpl implements NotificationRepository {

    private final NotificationJpaRepository notificationJpaRepository;

    @Override
    public Notification save(Notification notification) {
        return notificationJpaRepository.save(notification);
    }

    @Override
    public List<Notification> saveAll(List<Notification> notifications) {
        return notificationJpaRepository.saveAll(notifications);
    }

    /**
     * 정렬은 메서드 이름이 정합니다. 쪽 요청에는 정렬을 싣지 않습니다.
     * 실으면 이름의 정렬 뒤에 덧붙어 차례가 흔들립니다.
     */
    @Override
    public Page<Notification> findByAccountId(UUID accountId, int page, int size) {
        return notificationJpaRepository.findByAccountIdOrderByCreatedAtDescIdDesc(
                accountId, PageRequest.of(page, size));
    }

    @Override
    public Optional<Notification> findByIdAndAccountId(UUID notificationId, UUID accountId) {
        return notificationJpaRepository.findByIdAndAccountId(notificationId, accountId);
    }

    @Override
    public List<Notification> findUnreadByAccountId(UUID accountId) {
        return notificationJpaRepository.findByAccountIdAndReadAtIsNull(accountId);
    }

    @Override
    public long countUnreadByAccountId(UUID accountId) {
        return notificationJpaRepository.countByAccountIdAndReadAtIsNull(accountId);
    }

    @Override
    public int deleteUnreadPolicyChanged(Collection<UUID> accountIds, UUID placeId) {
        if (accountIds.isEmpty()) {
            return 0;
        }
        return notificationJpaRepository.deleteUnread(accountIds, placeId, NotifType.POLICY_CHANGED);
    }
}
