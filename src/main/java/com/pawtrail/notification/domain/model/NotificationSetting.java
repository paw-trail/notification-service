package com.pawtrail.notification.domain.model;

import com.pawtrail.common.entity.BaseEntity;
import com.pawtrail.notification.domain.enums.NotifType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.util.Objects;
import java.util.UUID;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * 한 사람의 알림 수신 설정입니다. 알림 종류마다 받을지를 담습니다.
 *
 * 행이 없으면 전부 받는 것으로 봅니다. 미리 만들지 않고 사용자가 설정을 처음 바꾸는 순간 생깁니다.
 *
 * 탈퇴 표시로도 씁니다. 탈퇴하면 이 행을 지우지 않고 deleted_at 을 찍어 남기고,
 * 탈퇴보다 늦게 도착한 알림거리는 이 표시를 보고 건너뜁니다.
 *
 * <b>@SQLRestriction 을 두지 않습니다.</b>
 * 이 표를 읽는 곳은 설정 API 와 알림을 만들 때 거르는 자리 둘뿐인데 거르는 자리는 탈퇴 표시를 봐야 합니다.
 * 숨겨서 얻는 조회가 없어 늘 표시까지 읽고 isDeleted() 로 가릅니다.
 * user 의 UserProfile 은 조회 대부분이 탈퇴한 사람을 숨겨야 해서 반대로 두었습니다.
 */
@Entity
@Table(name = "notification_setting")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class NotificationSetting extends BaseEntity {

    // 한 사람에 한 행이라 계정 식별자를 그대로 PK 로 씀 — 애플리케이션이 새로 만들지 않음
    @Id
    @Column(name = "account_id", columnDefinition = "uuid")
    private UUID accountId;

    @Column(name = "policy_changed", nullable = false)
    private boolean policyChanged;

    @Column(name = "report_resolved", nullable = false)
    private boolean reportResolved;

    /**
     * 설정을 처음 바꿀 때 만드는 행입니다. 전부 받는 상태에서 시작합니다.
     *
     * 행이 없을 때 전부 받는 것으로 보는 규칙과 출발점이 같아,
     * 행이 생기는 순간에 보낸 칸 말고는 동작이 바뀌지 않습니다.
     */
    public static NotificationSetting defaults(UUID accountId) {
        Objects.requireNonNull(accountId, "accountId");
        NotificationSetting setting = new NotificationSetting();
        setting.accountId = accountId;
        setting.policyChanged = true;
        setting.reportResolved = true;
        return setting;
    }

    /**
     * 보낸 칸만 바꿉니다. null 인 칸은 그대로 둡니다.
     *
     * 탈퇴 표시는 건드리지 않습니다.
     * 탈퇴 뒤 30분 동안은 이미 나간 토큰으로 이 요청이 닿을 수 있는데,
     * 여기서 표시가 지워지면 늦게 도착한 알림거리를 거르는 장치가 무너집니다.
     */
    public void change(Boolean policyChanged, Boolean reportResolved) {
        if (policyChanged != null) {
            this.policyChanged = policyChanged;
        }
        if (reportResolved != null) {
            this.reportResolved = reportResolved;
        }
    }

    /**
     * 그 종류의 알림을 받는지 봅니다. 탈퇴 표시가 있으면 어느 종류도 받지 않습니다.
     */
    public boolean receives(NotifType notifType) {
        Objects.requireNonNull(notifType, "notifType");
        if (isDeleted()) {
            return false;
        }
        return switch (notifType) {
            case POLICY_CHANGED -> policyChanged;
            case REPORT_RESOLVED -> reportResolved;
        };
    }
}
