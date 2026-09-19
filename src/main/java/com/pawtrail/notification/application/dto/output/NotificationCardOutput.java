package com.pawtrail.notification.application.dto.output;

import com.pawtrail.notification.domain.enums.NotifType;
import com.pawtrail.notification.domain.model.Notification;
import java.time.LocalDateTime;
import java.util.UUID;

/**
 * 알림 목록의 카드 하나입니다.
 *
 * placeName 은 비어 있을 수 있습니다.
 * place 를 못 불렀거나 그 장소가 없으면 이름만 비우고 카드는 그대로 냅니다.
 * 화면은 "장소 이름을 불러오지 못했습니다" 처럼 안내하면 됩니다.
 *
 * readAt 이 비어 있으면 안 읽은 알림입니다.
 * 종류(notifType)는 코드값 그대로 내보내고 아이콘은 화면이 고릅니다.
 * 카드를 누르면 화면이 placeId 로 장소 상세를 엽니다. 두 종류 모두 같습니다.
 */
public record NotificationCardOutput(UUID notificationId,
                                     NotifType notifType,
                                     UUID placeId,
                                     String placeName,
                                     String title,
                                     String body,
                                     LocalDateTime readAt,
                                     LocalDateTime createdAt) {

    public static NotificationCardOutput of(Notification notification, String placeName) {
        return new NotificationCardOutput(
                notification.getId(),
                notification.getNotifType(),
                notification.getPlaceId(),
                placeName,
                notification.getTitle(),
                notification.getBody(),
                notification.getReadAt(),
                notification.getCreatedAt());
    }
}
