package com.pawtrail.notification.infrastructure.persistence.jpa;

import com.pawtrail.notification.domain.model.NotificationSetting;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

/**
 * 수신 설정 표를 JPA 로 읽고 씁니다.
 *
 * 엔티티에 @SQLRestriction 이 없어 findById 가 탈퇴 표시가 찍힌 행도 돌려줍니다. 의도입니다.
 */
public interface NotificationSettingJpaRepository extends JpaRepository<NotificationSetting, UUID> {
}
