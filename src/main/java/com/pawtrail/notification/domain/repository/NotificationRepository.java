package com.pawtrail.notification.domain.repository;

import com.pawtrail.notification.domain.model.Notification;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.domain.Page;

/**
 * 알림을 저장하고 찾아오는 약속입니다.
 *
 * 이 인터페이스에는 JPA 라는 단어가 나오지 않습니다.
 * 무엇을 할 수 있는지만 적고 어떻게 하는지는 infrastructure 가 정합니다.
 */
public interface NotificationRepository {

    Notification save(Notification notification);

    List<Notification> saveAll(List<Notification> notifications);

    /**
     * 그 사람의 알림을 최신순으로 한 쪽 읽습니다.
     *
     * 만든 시각이 같으면 식별자 순으로 가립니다.
     * 순서가 정해져 있지 않으면 쪽을 넘길 때 같은 행이 두 번 나오거나 한 행이 빠집니다.
     * 식별자가 UUID 버전 7 이라 그 순서가 곧 만들어진 순서입니다.
     */
    Page<Notification> findByAccountId(UUID accountId, int page, int size);

    /**
     * 그 사람의 알림 한 건을 찾습니다. 남의 알림이면 빈 값입니다.
     *
     * 계정까지 함께 걸어 찾으므로 "있는데 남의 것" 과 "없음" 이 똑같이 보입니다.
     */
    Optional<Notification> findByIdAndAccountId(UUID notificationId, UUID accountId);

    /**
     * 그 사람의 안 읽은 알림을 전부 읽습니다. 모두 읽음이 하나씩 표시할 때 씁니다.
     */
    List<Notification> findUnreadByAccountId(UUID accountId);

    /**
     * 그 사람의 안 읽은 알림 수를 셉니다. 헤더 벨이 폴링으로 부릅니다.
     */
    long countUnreadByAccountId(UUID accountId);

    /**
     * 그 사람들에게 그 장소로 나간 안 읽은 조건 변경 알림을 지우고 지운 수를 돌려줍니다.
     *
     * 같은 장소의 조건이 거듭 바뀌면 새 알림으로 갈아 끼우려는 것입니다. 읽은 알림은 남깁니다.
     * 쓰기 트랜잭션 안에서만 부릅니다. 알림을 만드는 일은 Inbox 가 여는 트랜잭션 안에서 돕니다.
     */
    int deleteUnreadPolicyChanged(Collection<UUID> accountIds, UUID placeId);
}
