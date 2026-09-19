package com.pawtrail.notification.application.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.pawtrail.common.audit.AuditorProvider;
import com.pawtrail.notification.domain.model.NotificationSetting;
import com.pawtrail.notification.domain.repository.NotificationRepository;
import com.pawtrail.notification.domain.repository.NotificationSettingRepository;
import com.pawtrail.notification.domain.repository.WithdrawalLockRepository;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InOrder;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

/**
 * 탈퇴 처리의 세 갈래와, 갈래와 상관없이 알림을 지우는지를 검사합니다. 저장소는 흉내 냅니다.
 */
@ExtendWith(MockitoExtension.class)
class AccountWithdrawnServiceTest {

    private static final UUID ACCOUNT = UUID.fromString("01999999-0000-7000-8000-000000000001");

    @Mock
    private NotificationRepository notificationRepository;

    @Mock
    private NotificationSettingRepository notificationSettingRepository;

    @Mock
    private AuditorProvider auditorProvider;

    @Mock
    private WithdrawalLockRepository withdrawalLockRepository;

    @InjectMocks
    private AccountWithdrawnService service;

    @Captor
    private ArgumentCaptor<NotificationSetting> settingCaptor;

    @Test
    @DisplayName("설정 행이 없으면 탈퇴 표시 행을 새로 만들고 알림을 지운다")
    void 행_없음() {
        when(auditorProvider.current()).thenReturn("SYSTEM");
        when(notificationSettingRepository.findByAccountId(ACCOUNT)).thenReturn(Optional.empty());
        when(notificationRepository.deleteAllByAccountId(ACCOUNT)).thenReturn(2);

        service.withdraw(ACCOUNT);

        // 탈퇴 잠금을 설정 읽기 · 알림 지우기보다 먼저 잡아야 함
        InOrder order = inOrder(withdrawalLockRepository, notificationSettingRepository, notificationRepository);
        order.verify(withdrawalLockRepository).lockForWithdrawal();
        order.verify(notificationSettingRepository).findByAccountId(ACCOUNT);
        order.verify(notificationRepository).deleteAllByAccountId(ACCOUNT);

        verify(notificationSettingRepository).save(settingCaptor.capture());
        assertThat(settingCaptor.getValue().getAccountId()).isEqualTo(ACCOUNT);
        assertThat(settingCaptor.getValue().isDeleted()).isTrue();
        verify(notificationRepository).deleteAllByAccountId(ACCOUNT);
    }

    @Test
    @DisplayName("정상 행이면 탈퇴 표시만 찍고 칸 값은 그대로 두며, 저장을 따로 부르지 않는다")
    void 정상_행() {
        NotificationSetting setting = NotificationSetting.defaults(ACCOUNT);
        setting.change(false, null);
        when(auditorProvider.current()).thenReturn("SYSTEM");
        when(notificationSettingRepository.findByAccountId(ACCOUNT)).thenReturn(Optional.of(setting));
        when(notificationRepository.deleteAllByAccountId(ACCOUNT)).thenReturn(0);

        service.withdraw(ACCOUNT);

        assertThat(setting.isDeleted()).isTrue();
        assertThat(setting.isPolicyChanged()).isFalse();
        verify(notificationSettingRepository, never()).save(any());
        verify(notificationRepository).deleteAllByAccountId(ACCOUNT);
    }

    @Test
    @DisplayName("이미 탈퇴 표시가 있으면 설정은 건드리지 않고 알림은 그래도 지운다")
    void 이미_표시() {
        NotificationSetting marked = NotificationSetting.withdrawnMarker(ACCOUNT, "SYSTEM");
        when(auditorProvider.current()).thenReturn("SYSTEM");
        when(notificationSettingRepository.findByAccountId(ACCOUNT)).thenReturn(Optional.of(marked));
        when(notificationRepository.deleteAllByAccountId(ACCOUNT)).thenReturn(1);

        service.withdraw(ACCOUNT);

        assertThat(marked.isDeleted()).isTrue();
        verify(notificationSettingRepository, never()).save(any());
        verify(notificationRepository).deleteAllByAccountId(ACCOUNT);
    }
}
