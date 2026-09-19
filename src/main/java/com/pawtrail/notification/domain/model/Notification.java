package com.pawtrail.notification.domain.model;

import com.pawtrail.common.entity.BaseEntity;
import com.pawtrail.notification.domain.enums.NotifType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.LocalDateTime;
import java.util.Objects;
import java.util.UUID;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.UuidGenerator;

/**
 * 한 사람에게 보낸 알림 한 건입니다.
 *
 * 문구(제목 · 본문)는 만들 때 정해지고 바뀌지 않습니다. 바뀌는 것은 읽은 시각 하나뿐입니다.
 * 장소 이름은 담지 않습니다. place 가 바꿀 수 있는 값이라 목록을 열 때 받아 채웁니다.
 *
 * 지울 때는 행째 지웁니다. 공통 규약의 소프트 딜리트 칸은 쓰지 않습니다.
 * 탈퇴한 계정의 알림과, 같은 장소의 새 조건 알림으로 갈아 끼운 알림이 그 대상입니다.
 */
@Entity
@Table(name = "notification")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Notification extends BaseEntity {

    // 표의 폭과 같음 — 넘기면 DB 가 막기 전에 여기서 막음
    // 본문 폭은 문구를 짓는 쪽이 긴 메모를 자를 때도 씀
    public static final int TITLE_MAX = 100;
    public static final int BODY_MAX = 500;

    @Id
    @UuidGenerator(style = UuidGenerator.Style.VERSION_7)
    @Column(columnDefinition = "uuid")
    private UUID id;

    @Column(name = "account_id", nullable = false)
    private UUID accountId;

    @Enumerated(EnumType.STRING)
    @Column(name = "notif_type", nullable = false, length = 24)
    private NotifType notifType;

    @Column(name = "place_id", nullable = false)
    private UUID placeId;

    @Column(nullable = false, length = TITLE_MAX)
    private String title;

    @Column(nullable = false, length = BODY_MAX)
    private String body;

    @Column(name = "read_at")
    private LocalDateTime readAt;

    /**
     * 새 알림을 만듭니다. 문구는 부르는 쪽이 종류에 맞게 지어 넘깁니다.
     *
     * 값이 비었거나 표의 폭을 넘으면 IllegalArgumentException 을 던집니다.
     * 사용자 입력이 아니라 우리 코드가 지은 문구라 어기면 버그이고, 그래서 500 으로 드러나게 둡니다.
     */
    public static Notification create(UUID accountId,
                                      NotifType notifType,
                                      UUID placeId,
                                      String title,
                                      String body) {
        Objects.requireNonNull(accountId, "accountId");
        Objects.requireNonNull(notifType, "notifType");
        Objects.requireNonNull(placeId, "placeId");
        requireText(title, TITLE_MAX, "title");
        requireText(body, BODY_MAX, "body");

        Notification notification = new Notification();
        notification.accountId = accountId;
        notification.notifType = notifType;
        notification.placeId = placeId;
        notification.title = title;
        notification.body = body;
        return notification;
    }

    /**
     * 읽음으로 표시합니다.
     *
     * 이미 읽은 알림이면 아무것도 바꾸지 않습니다. 처음 읽은 시각을 남기기 위해서입니다.
     * 같은 알림을 두 번 눌러도 결과가 같아 화면이 따로 가를 필요가 없습니다.
     *
     * @return 이번에 읽음으로 바뀌었으면 true
     */
    public boolean markRead(LocalDateTime now) {
        Objects.requireNonNull(now, "now");
        if (readAt != null) {
            return false;
        }
        this.readAt = now;
        return true;
    }

    public boolean isRead() {
        return readAt != null;
    }

    private static void requireText(String value, int max, String name) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(name + " 가 비어 있습니다");
        }
        if (value.length() > max) {
            throw new IllegalArgumentException(name + " 가 " + max + "자를 넘습니다: " + value.length());
        }
    }
}
