package com.pawtrail.notification.application.service;

import com.pawtrail.common.audit.AuditorProvider;
import com.pawtrail.notification.domain.model.NotificationSetting;
import com.pawtrail.notification.domain.repository.NotificationRepository;
import com.pawtrail.notification.domain.repository.NotificationSettingRepository;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

/**
 * 탈퇴한 계정에 대해 이 서비스가 가진 것을 정리합니다.
 *
 * account.withdrawn 을 받는 경로에서만 불립니다. 사용자가 직접 부르는 API 는 없습니다.
 *
 * 하는 일이 둘입니다.
 *   알림          행째 전부 지웁니다. 그 사람이 받은 알림이라 남길 이유가 없습니다.
 *   수신 설정      지우지 않고 탈퇴 표시를 찍어 남깁니다.
 *
 * 설정 행을 남기는 이유는 이벤트 사이에 순서가 없기 때문입니다.
 * 탈퇴와 제보 결과 · 조건 변경은 토픽이 달라, 탈퇴보다 늦게 도착한 알림거리가 있을 수 있습니다.
 * 그것을 받으면 알림을 만들기 전에 설정을 보는데, 그때 탈퇴 표시를 보고 건너뜁니다.
 * 행을 지우면 "설정 없음 = 전부 받음" 이 되어 탈퇴한 사람에게 알림이 다시 생깁니다.
 * user 가 프로필에 탈퇴 표시 행을 남기는 것과 같은 모양입니다.
 *
 * @Transactional 을 붙이지 않습니다.
 * 이벤트 경로는 InboxProcessor.processOnce 가 이미 트랜잭션을 열고 있습니다.
 * 트랜잭션 없이 불리면 아래 한 문장 삭제가 그 자리에서 실패하므로 경계가 사라진 것을 모르고 넘어갈 일은 없습니다.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AccountWithdrawnService {

    private final NotificationRepository notificationRepository;
    private final NotificationSettingRepository notificationSettingRepository;
    private final AuditorProvider auditorProvider;

    /**
     * 설정 행에 탈퇴 표시를 남기고 알림을 지웁니다.
     *
     * 설정 행의 상태가 셋입니다. 조회 한 번으로 가릅니다(설정 표는 탈퇴 표시 행도 돌려줌).
     *
     *   행이 아예 없음   설정을 한 번도 안 바꾼 사람입니다. 표시 행을 새로 만듭니다.
     *   이미 탈퇴 표시   같은 이벤트가 다시 온 것은 Inbox 가 거르므로, 대개 다른 경로로 이미 찍힌 경우입니다.
     *                  건드리지 않습니다. 다시 찍으면 삭제 시각만 흔들립니다.
     *   정상            탈퇴 표시를 찍습니다. 칸 값은 그대로 둡니다.
     *
     * 알림 삭제는 갈래와 상관없이 늘 합니다.
     * 설정 행이 없어도 알림은 있을 수 있기 때문입니다. 설정을 안 바꾼 사람도 알림은 받습니다.
     *
     * 설정을 먼저 다룹니다. 알림 삭제가 한 문장이라 영속성 컨텍스트를 거치지 않으므로
     * 순서가 보이는 대로 나가야 하고, 그 문장이 밀린 쓰기를 먼저 반영하도록 해 두었습니다.
     *
     * 정상 행에서 save 를 부르지 않습니다. 조회해 온 엔티티라 커밋 때 변경 감지가 UPDATE 를 냅니다.
     */
    public void withdraw(UUID accountId) {
        String deletedBy = auditorProvider.current();

        NotificationSetting setting = notificationSettingRepository.findByAccountId(accountId).orElse(null);

        if (setting == null) {
            notificationSettingRepository.save(NotificationSetting.withdrawnMarker(accountId, deletedBy));
            log.info("설정 행이 없어 탈퇴 표시 행을 만들었습니다: accountId={}", accountId);

        } else if (setting.isDeleted()) {
            log.info("이미 탈퇴 표시가 있어 설정은 그대로 둡니다: accountId={}", accountId);

        } else {
            setting.delete(deletedBy);
            log.info("설정 행에 탈퇴 표시를 찍었습니다: accountId={}", accountId);
        }

        int notifications = notificationRepository.deleteAllByAccountId(accountId);
        log.info("탈퇴한 계정의 알림을 지웠습니다: accountId={}, notification={}", accountId, notifications);
    }
}
