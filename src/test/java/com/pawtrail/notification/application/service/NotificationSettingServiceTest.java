package com.pawtrail.notification.application.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.pawtrail.common.exception.CommonErrorCode;
import com.pawtrail.common.exception.CustomException;
import com.pawtrail.notification.application.dto.input.NotificationSettingUpdateInput;
import com.pawtrail.notification.application.dto.output.NotificationSettingOutput;
import com.pawtrail.notification.domain.model.NotificationSetting;
import com.pawtrail.notification.domain.repository.NotificationSettingRepository;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

/**
 * 수신 설정 조회와 부분 수정을 검사합니다. 저장소는 흉내 냅니다.
 */
@ExtendWith(MockitoExtension.class)
class NotificationSettingServiceTest {

    private static final UUID ACCOUNT = UUID.fromString("01999999-0000-7000-8000-000000000001");

    @Mock
    private NotificationSettingRepository notificationSettingRepository;

    @InjectMocks
    private NotificationSettingService notificationSettingService;

    @Captor
    private ArgumentCaptor<NotificationSetting> settingCaptor;

    @Test
    @DisplayName("행이 없으면 둘 다 켜진 값을 돌려준다")
    void 행_없음() {
        when(notificationSettingRepository.findByAccountId(ACCOUNT)).thenReturn(Optional.empty());

        NotificationSettingOutput output = notificationSettingService.get(ACCOUNT);

        assertThat(output.policyChanged()).isTrue();
        assertThat(output.reportResolved()).isTrue();
    }

    @Test
    @DisplayName("행이 있으면 칸 값을 그대로 돌려준다")
    void 행_있음() {
        NotificationSetting setting = NotificationSetting.defaults(ACCOUNT);
        setting.change(null, false);
        when(notificationSettingRepository.findByAccountId(ACCOUNT)).thenReturn(Optional.of(setting));

        NotificationSettingOutput output = notificationSettingService.get(ACCOUNT);

        assertThat(output.policyChanged()).isTrue();
        assertThat(output.reportResolved()).isFalse();
    }

    @Test
    @DisplayName("바꿀 칸이 하나도 없으면 400 이고 행을 만들지 않는다")
    void 빈_요청() {
        assertThatThrownBy(() -> notificationSettingService.update(ACCOUNT, new NotificationSettingUpdateInput(null, null)))
                .isInstanceOfSatisfying(CustomException.class,
                        e -> assertThat(e.getErrorCode()).isEqualTo(CommonErrorCode.VALIDATION_FAILED));
        verify(notificationSettingRepository, never()).save(any());
    }

    @Test
    @DisplayName("처음 바꾸면 둘 다 켜진 행을 만든 뒤 보낸 칸만 얹는다")
    void 처음_바꿈() {
        when(notificationSettingRepository.findByAccountId(ACCOUNT)).thenReturn(Optional.empty());
        when(notificationSettingRepository.save(settingCaptor.capture()))
                .thenAnswer(invocation -> invocation.getArgument(0));

        NotificationSettingOutput output =
                notificationSettingService.update(ACCOUNT, new NotificationSettingUpdateInput(false, null));

        assertThat(settingCaptor.getValue().getAccountId()).isEqualTo(ACCOUNT);
        assertThat(output.policyChanged()).isFalse();
        assertThat(output.reportResolved()).isTrue();
    }

    @Test
    @DisplayName("탈퇴 표시가 찍힌 행은 칸만 바뀌고 표시는 남는다")
    void 탈퇴_표시_유지() {
        NotificationSetting marked = NotificationSetting.defaults(ACCOUNT);
        marked.delete("SYSTEM");
        when(notificationSettingRepository.findByAccountId(ACCOUNT)).thenReturn(Optional.of(marked));
        when(notificationSettingRepository.save(settingCaptor.capture()))
                .thenAnswer(invocation -> invocation.getArgument(0));

        notificationSettingService.update(ACCOUNT, new NotificationSettingUpdateInput(null, false));

        assertThat(settingCaptor.getValue().isDeleted()).isTrue();
        assertThat(settingCaptor.getValue().isReportResolved()).isFalse();
    }
}
