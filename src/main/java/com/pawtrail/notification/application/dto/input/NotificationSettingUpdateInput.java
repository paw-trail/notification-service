package com.pawtrail.notification.application.dto.input;

/**
 * 수신 설정을 바꾸는 요청을 서비스가 받는 모양입니다.
 *
 * 칸이 null 이면 그 칸을 바꾸지 않습니다.
 * 토글은 null 이 뜻을 가질 자리가 없어, 안 보낸 것과 null 을 보낸 것을 가르지 않습니다.
 *
 * @param policyChanged  조건 변경 알림을 받을지입니다. null 이면 그대로 둡니다.
 * @param reportResolved 제보 처리 결과 알림을 받을지입니다. null 이면 그대로 둡니다.
 */
public record NotificationSettingUpdateInput(Boolean policyChanged, Boolean reportResolved) {

    /**
     * 바꿀 칸이 하나도 없는지 봅니다. 그런 요청은 400 으로 막습니다.
     */
    public boolean isEmpty() {
        return policyChanged == null && reportResolved == null;
    }
}
