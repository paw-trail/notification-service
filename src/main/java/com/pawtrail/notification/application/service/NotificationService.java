package com.pawtrail.notification.application.service;

import com.pawtrail.common.exception.CustomException;
import com.pawtrail.common.response.PageResponse;
import com.pawtrail.notification.application.dto.output.NotificationCardOutput;
import com.pawtrail.notification.application.dto.output.UnreadCountOutput;
import com.pawtrail.notification.domain.exception.NotificationErrorCode;
import com.pawtrail.notification.domain.model.Notification;
import com.pawtrail.notification.domain.provider.PlaceProvider;
import com.pawtrail.notification.domain.provider.dto.PlaceData;
import com.pawtrail.notification.domain.repository.NotificationRepository;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 사용자가 자기 알림을 보고 읽는 일을 맡습니다.
 *
 * 알림을 만드는 일은 여기 두지 않습니다.
 * 부르는 쪽이 사용자 요청이 아니라 이벤트 소비자이고, 받는 사람을 모으는 일이 붙습니다.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class NotificationService {

    private final NotificationRepository notificationRepository;
    private final PlaceProvider placeProvider;

    /**
     * 내 알림을 최신순으로 한 쪽 돌려줍니다. 헤더 벨을 누르면 부릅니다.
     *
     * 장소 이름은 place 에서 받아 채웁니다.
     * 못 받으면 이름만 비우고 목록은 그대로 냅니다. 알림은 제목과 본문만으로도 읽힙니다.
     *
     * 한 쪽이 비어 있으면 place 를 부르지 않습니다. 물어볼 것이 없습니다.
     */
    @Transactional(readOnly = true)
    public PageResponse<NotificationCardOutput> getMine(UUID accountId, int page, int size) {
        Page<Notification> notifications = notificationRepository.findByAccountId(accountId, page, size);
        Map<UUID, PlaceData> places = findPlaces(notifications.getContent());
        return PageResponse.from(notifications, notification ->
                NotificationCardOutput.of(notification, nameOf(places, notification.getPlaceId())));
    }

    /**
     * 알림 하나를 읽음으로 표시합니다.
     *
     * 이미 읽은 알림이면 아무것도 바꾸지 않고 성공으로 끝냅니다. 처음 읽은 시각을 남깁니다.
     * 없거나 남의 알림이면 404 NOTIFICATION_NOT_FOUND 입니다. 둘을 가르지 않습니다.
     */
    @Transactional
    public void read(UUID accountId, UUID notificationId) {
        Notification notification = notificationRepository.findByIdAndAccountId(notificationId, accountId)
                .orElseThrow(() -> {
                    log.info("읽을 알림이 없습니다: accountId={}, notificationId={}", accountId, notificationId);
                    return new CustomException(NotificationErrorCode.NOTIFICATION_NOT_FOUND);
                });

        boolean changed = notification.markRead(LocalDateTime.now());
        if (changed) {
            log.info("알림을 읽었습니다: accountId={}, notificationId={}", accountId, notificationId);
        }
    }

    /**
     * 안 읽은 알림을 전부 읽음으로 표시합니다. 읽을 것이 없어도 성공입니다.
     *
     * 한 건씩 엔티티로 표시합니다. 한 문장으로 고치는 벌크 UPDATE 를 쓰지 않습니다.
     * 벌크 UPDATE 는 감사 칸(updated_at · updated_by)을 자동으로 채우지 않고,
     * 한 사람의 안 읽은 알림이라 수가 적어 하나씩 해도 부담이 없습니다.
     */
    @Transactional
    public void readAll(UUID accountId) {
        List<Notification> unread = notificationRepository.findUnreadByAccountId(accountId);
        LocalDateTime now = LocalDateTime.now();
        unread.forEach(notification -> notification.markRead(now));
        log.info("알림을 모두 읽었습니다: accountId={}, count={}", accountId, unread.size());
    }

    /**
     * 안 읽은 알림 수를 돌려줍니다. 헤더 벨이 30초에서 1분마다 부릅니다.
     *
     * 다른 서비스를 부르지 않습니다. 자주 불리는 자리라 DB 만 봅니다.
     */
    @Transactional(readOnly = true)
    public UnreadCountOutput countUnread(UUID accountId) {
        return new UnreadCountOutput(notificationRepository.countUnreadByAccountId(accountId));
    }

    private Map<UUID, PlaceData> findPlaces(List<Notification> notifications) {
        if (notifications.isEmpty()) {
            return Map.of();
        }
        List<UUID> placeIds = notifications.stream().map(Notification::getPlaceId).distinct().toList();
        Map<UUID, PlaceData> places = placeProvider.findByIds(placeIds);
        if (places == null) {
            log.warn("장소 이름을 받지 못해 이름 없이 목록을 냅니다: 장소 {}곳", placeIds.size());
            return Map.of();
        }
        return places;
    }

    private static String nameOf(Map<UUID, PlaceData> places, UUID placeId) {
        PlaceData place = places.get(placeId);
        return place == null ? null : place.name();
    }
}
