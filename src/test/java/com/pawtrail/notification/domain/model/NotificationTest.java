package com.pawtrail.notification.domain.model;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.pawtrail.notification.domain.enums.NotifType;
import java.time.LocalDateTime;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * 알림 한 건이 만들어지는 규칙과 읽음 표시를 검사합니다.
 */
class NotificationTest {

    private static final UUID ACCOUNT = UUID.fromString("01999999-0000-7000-8000-000000000001");
    private static final UUID PLACE = UUID.fromString("01999999-0000-7000-8000-00000000aaaa");

    @Test
    @DisplayName("만들면 안 읽은 알림이고 넘긴 값이 그대로 담긴다")
    void 만들기() {
        Notification notification = Notification.create(
                ACCOUNT, NotifType.POLICY_CHANGED, PLACE, "동반 조건이 바뀌었습니다", "체중 제한");

        assertThat(notification.getAccountId()).isEqualTo(ACCOUNT);
        assertThat(notification.getNotifType()).isEqualTo(NotifType.POLICY_CHANGED);
        assertThat(notification.getPlaceId()).isEqualTo(PLACE);
        assertThat(notification.getReadAt()).isNull();
        assertThat(notification.isRead()).isFalse();
    }

    @Test
    @DisplayName("제목이나 본문이 비면 만들지 않는다")
    void 빈_문구() {
        assertThatThrownBy(() -> Notification.create(ACCOUNT, NotifType.REPORT_RESOLVED, PLACE, " ", "메모"))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> Notification.create(ACCOUNT, NotifType.REPORT_RESOLVED, PLACE, "제목", null))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    @DisplayName("표의 폭을 넘는 문구는 만들지 않는다 — 제목 100자 · 본문 500자까지")
    void 폭() {
        String title100 = "가".repeat(100);
        String body500 = "나".repeat(500);

        Notification notification = Notification.create(ACCOUNT, NotifType.REPORT_RESOLVED, PLACE, title100, body500);
        assertThat(notification.getTitle()).isEqualTo(title100);

        assertThatThrownBy(() -> Notification.create(ACCOUNT, NotifType.REPORT_RESOLVED, PLACE, title100 + "가", "메모"))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> Notification.create(ACCOUNT, NotifType.REPORT_RESOLVED, PLACE, "제목", body500 + "나"))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    @DisplayName("두 번 읽어도 처음 읽은 시각이 남는다")
    void 읽음_한_번() {
        Notification notification = Notification.create(
                ACCOUNT, NotifType.REPORT_RESOLVED, PLACE, "제보하신 내용이 반영되었습니다", "고쳤습니다");
        LocalDateTime first = LocalDateTime.of(2026, 9, 20, 10, 0);
        LocalDateTime second = LocalDateTime.of(2026, 9, 20, 11, 0);

        boolean firstChanged = notification.markRead(first);
        boolean secondChanged = notification.markRead(second);

        assertThat(firstChanged).isTrue();
        assertThat(secondChanged).isFalse();
        assertThat(notification.getReadAt()).isEqualTo(first);
    }
}
