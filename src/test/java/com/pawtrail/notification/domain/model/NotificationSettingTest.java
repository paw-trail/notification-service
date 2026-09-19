package com.pawtrail.notification.domain.model;

import static org.assertj.core.api.Assertions.assertThat;

import com.pawtrail.notification.domain.enums.NotifType;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * 수신 설정의 기본값 · 보낸 칸만 바꾸기 · 탈퇴 표시를 검사합니다.
 */
class NotificationSettingTest {

    private static final UUID ACCOUNT = UUID.fromString("01999999-0000-7000-8000-000000000001");

    @Test
    @DisplayName("처음 만든 설정은 둘 다 켜져 있다 — 행이 없을 때와 같은 출발점")
    void 기본값() {
        NotificationSetting setting = NotificationSetting.defaults(ACCOUNT);

        assertThat(setting.isPolicyChanged()).isTrue();
        assertThat(setting.isReportResolved()).isTrue();
        assertThat(setting.receives(NotifType.POLICY_CHANGED)).isTrue();
        assertThat(setting.receives(NotifType.REPORT_RESOLVED)).isTrue();
    }

    @Test
    @DisplayName("보낸 칸만 바뀌고 비어 있는 칸은 그대로다")
    void 보낸_칸만() {
        NotificationSetting setting = NotificationSetting.defaults(ACCOUNT);

        setting.change(false, null);

        assertThat(setting.receives(NotifType.POLICY_CHANGED)).isFalse();
        assertThat(setting.receives(NotifType.REPORT_RESOLVED)).isTrue();
    }

    @Test
    @DisplayName("탈퇴 표시가 찍히면 어느 종류도 받지 않고, 칸을 바꿔도 표시는 남는다")
    void 탈퇴_표시() {
        NotificationSetting setting = NotificationSetting.defaults(ACCOUNT);
        setting.delete("SYSTEM");

        setting.change(true, true);

        assertThat(setting.isDeleted()).isTrue();
        assertThat(setting.receives(NotifType.POLICY_CHANGED)).isFalse();
        assertThat(setting.receives(NotifType.REPORT_RESOLVED)).isFalse();
    }

    @Test
    @DisplayName("탈퇴 표시 행은 표시가 찍힌 채로 만들어져 어느 종류도 받지 않는다")
    void 탈퇴_표시_행() {
        NotificationSetting marker = NotificationSetting.withdrawnMarker(ACCOUNT, "SYSTEM");

        assertThat(marker.getAccountId()).isEqualTo(ACCOUNT);
        assertThat(marker.isDeleted()).isTrue();
        assertThat(marker.receives(NotifType.POLICY_CHANGED)).isFalse();
        assertThat(marker.receives(NotifType.REPORT_RESOLVED)).isFalse();
    }
}
