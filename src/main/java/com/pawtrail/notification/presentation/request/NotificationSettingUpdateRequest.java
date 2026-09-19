package com.pawtrail.notification.presentation.request;

import com.pawtrail.notification.application.dto.input.NotificationSettingUpdateInput;

/**
 * PATCH /api/v1/notifications/settings 의 요청 본문입니다.
 *
 * 보낸 칸만 바꿉니다. 둘 다 안 보내면 서비스가 400 으로 막습니다.
 *
 * user 의 일정 수정 요청은 안 보낸 것과 null 을 가르려고 세터 플래그를 두었으나 여기서는 두지 않습니다.
 * 토글은 null 이 뜻을 가질 자리가 없어 둘을 같게 봐도 되기 때문입니다.
 *
 * @param policyChanged  조건 변경 알림을 받을지입니다.
 * @param reportResolved 제보 처리 결과 알림을 받을지입니다.
 */
public record NotificationSettingUpdateRequest(Boolean policyChanged, Boolean reportResolved) {

    public NotificationSettingUpdateInput toInput() {
        return new NotificationSettingUpdateInput(policyChanged, reportResolved);
    }
}
