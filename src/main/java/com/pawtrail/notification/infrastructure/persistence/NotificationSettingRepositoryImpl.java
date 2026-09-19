package com.pawtrail.notification.infrastructure.persistence;

import com.pawtrail.notification.domain.model.NotificationSetting;
import com.pawtrail.notification.domain.repository.NotificationSettingRepository;
import com.pawtrail.notification.infrastructure.persistence.jpa.NotificationSettingJpaRepository;
import java.util.Optional;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

/**
 * 도메인이 선언한 수신 설정 저장소 약속을 JPA 로 구현합니다.
 */
@Repository
@RequiredArgsConstructor
public class NotificationSettingRepositoryImpl implements NotificationSettingRepository {

    private final NotificationSettingJpaRepository notificationSettingJpaRepository;

    /**
     * 저장합니다.
     *
     * 식별자가 계정이라 새 행도 식별자를 들고 들어옵니다.
     * 그래서 스프링이 새 행인지 모르고 먼저 조회한 뒤 없으면 넣습니다.
     * 조회가 한 번 더 붙지만 설정을 처음 바꿀 때 한 번뿐이라 그대로 둡니다.
     */
    @Override
    public NotificationSetting save(NotificationSetting setting) {
        return notificationSettingJpaRepository.save(setting);
    }

    @Override
    public Optional<NotificationSetting> findByAccountId(UUID accountId) {
        return notificationSettingJpaRepository.findById(accountId);
    }
}
