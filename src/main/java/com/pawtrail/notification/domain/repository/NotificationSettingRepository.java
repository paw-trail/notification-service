package com.pawtrail.notification.domain.repository;

import com.pawtrail.notification.domain.model.NotificationSetting;
import java.util.Optional;
import java.util.UUID;

/**
 * 수신 설정을 저장하고 찾아오는 약속입니다.
 */
public interface NotificationSettingRepository {

    NotificationSetting save(NotificationSetting setting);

    /**
     * 그 사람의 설정 행을 찾습니다. 탈퇴 표시가 찍힌 행도 돌려줍니다.
     *
     * 행이 없으면 빈 값이고, 부르는 쪽은 그것을 "전부 받음" 으로 봅니다.
     */
    Optional<NotificationSetting> findByAccountId(UUID accountId);
}
