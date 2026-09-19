package com.pawtrail.notification.application.service;

import com.pawtrail.common.exception.CommonErrorCode;
import com.pawtrail.common.exception.CustomException;
import com.pawtrail.notification.application.dto.input.NotificationSettingUpdateInput;
import com.pawtrail.notification.application.dto.output.NotificationSettingOutput;
import com.pawtrail.notification.domain.model.NotificationSetting;
import com.pawtrail.notification.domain.repository.NotificationSettingRepository;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 알림 수신 설정을 보여 주고 바꾸는 일을 맡습니다.
 *
 * 행이 없으면 전부 받는 것으로 봅니다. 미리 만들지 않고 처음 바꾸는 순간 만듭니다.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class NotificationSettingService {

    private final NotificationSettingRepository notificationSettingRepository;

    /**
     * 설정을 돌려줍니다. 행이 없으면 둘 다 켜진 값입니다.
     */
    @Transactional(readOnly = true)
    public NotificationSettingOutput get(UUID accountId) {
        return notificationSettingRepository.findByAccountId(accountId)
                .map(NotificationSettingOutput::from)
                .orElseGet(NotificationSettingOutput::allOn);
    }

    /**
     * 보낸 칸만 바꾸고 바뀐 설정 전체를 돌려줍니다.
     *
     * 바꿀 칸이 하나도 없으면 400 VALIDATION_FAILED 입니다.
     * 아무것도 안 바뀌는 요청이 행만 만들고 끝나는 것을 막습니다.
     *
     * 행이 없으면 전부 켜진 행을 만든 뒤 보낸 칸을 얹습니다.
     * 두 요청이 동시에 처음 행을 만들면 뒤의 것이 기본 키에 부딪혀 500 이 날 수 있습니다.
     * 같은 사람이 설정을 밀리초 안에 두 번 바꿔야 닿는 자리이고,
     * 부딪혀도 앞 요청이 만든 행 하나만 남아 받아들입니다. user 즐겨찾기 담기와 같은 판단입니다.
     *
     * 탈퇴 표시가 찍힌 행도 칸만 바꾸고 표시는 그대로 둡니다. 이유는 NotificationSetting.change 에 있습니다.
     */
    @Transactional
    public NotificationSettingOutput update(UUID accountId, NotificationSettingUpdateInput input) {
        if (input.isEmpty()) {
            log.info("바꿀 설정이 없는 요청입니다: accountId={}", accountId);
            throw new CustomException(CommonErrorCode.VALIDATION_FAILED);
        }

        NotificationSetting setting = notificationSettingRepository.findByAccountId(accountId)
                .orElseGet(() -> NotificationSetting.defaults(accountId));
        setting.change(input.policyChanged(), input.reportResolved());
        NotificationSetting saved = notificationSettingRepository.save(setting);

        log.info("알림 설정을 바꿨습니다: accountId={}, policyChanged={}, reportResolved={}",
                accountId, saved.isPolicyChanged(), saved.isReportResolved());
        return NotificationSettingOutput.from(saved);
    }
}
