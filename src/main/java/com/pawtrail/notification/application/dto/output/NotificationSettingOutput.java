package com.pawtrail.notification.application.dto.output;

import com.pawtrail.notification.domain.model.NotificationSetting;

/**
 * 한 사람의 알림 수신 설정입니다. 설정 화면의 토글 둘입니다.
 *
 * @param policyChanged  조건 변경 알림을 받는지입니다.
 * @param reportResolved 제보 처리 결과 알림을 받는지입니다.
 */
public record NotificationSettingOutput(boolean policyChanged, boolean reportResolved) {

    /**
     * 설정 행이 없는 사람의 값입니다. 행이 없으면 전부 받는 것으로 봅니다.
     */
    public static NotificationSettingOutput allOn() {
        return new NotificationSettingOutput(true, true);
    }

    /**
     * 설정 행의 칸 값을 그대로 옮깁니다.
     *
     * 탈퇴 표시가 찍힌 행이어도 칸 값을 그대로 보입니다.
     * 탈퇴 뒤 30분 동안 이미 나간 토큰으로만 닿는 자리이고, 알림은 표시 때문에 어차피 만들어지지 않습니다.
     */
    public static NotificationSettingOutput from(NotificationSetting setting) {
        return new NotificationSettingOutput(setting.isPolicyChanged(), setting.isReportResolved());
    }
}
