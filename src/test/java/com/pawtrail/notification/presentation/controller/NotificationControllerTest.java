package com.pawtrail.notification.presentation.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.pawtrail.common.enums.Role;
import com.pawtrail.common.response.CommonApiResponse;
import com.pawtrail.common.response.PageResponse;
import com.pawtrail.common.security.principal.CustomUserPrincipal;
import com.pawtrail.notification.application.dto.input.NotificationSettingUpdateInput;
import com.pawtrail.notification.application.dto.output.NotificationCardOutput;
import com.pawtrail.notification.application.dto.output.NotificationSettingOutput;
import com.pawtrail.notification.application.dto.output.UnreadCountOutput;
import com.pawtrail.notification.application.service.NotificationService;
import com.pawtrail.notification.application.service.NotificationSettingService;
import com.pawtrail.notification.presentation.request.NotificationSettingUpdateRequest;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

/**
 * 요청이 서비스 입력으로 바뀌는 자리와 응답 상태를 검사합니다.
 *
 * 컨트롤러를 직접 부르고 서비스는 흉내 냅니다.
 * 컨트롤러가 조립하는 자리는 직접 부르는 검사를 둡니다 — search 에서 여기서 터진 적이 있습니다.
 */
@ExtendWith(MockitoExtension.class)
class NotificationControllerTest {

    private static final UUID ACCOUNT = UUID.fromString("01999999-0000-7000-8000-000000000001");
    private static final UUID NOTIFICATION = UUID.fromString("01999999-0000-7000-8000-000000000101");

    private final CustomUserPrincipal principal = new CustomUserPrincipal(ACCOUNT, Role.USER);

    @Mock
    private NotificationService notificationService;

    @Mock
    private NotificationSettingService notificationSettingService;

    @InjectMocks
    private NotificationController controller;

    @Captor
    private ArgumentCaptor<NotificationSettingUpdateInput> inputCaptor;

    @Test
    @DisplayName("목록은 쪽 번호와 크기를 그대로 넘긴다")
    void 목록() {
        PageResponse<NotificationCardOutput> empty =
                new PageResponse<>(List.of(), new PageResponse.PageInfo(2, 5, 0, 0));
        when(notificationService.getMine(ACCOUNT, 2, 5)).thenReturn(empty);

        ResponseEntity<CommonApiResponse<PageResponse<NotificationCardOutput>>> response =
                controller.getMine(principal, 2, 5);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody().getData()).isSameAs(empty);
    }

    @Test
    @DisplayName("읽음은 내 계정과 알림 식별자를 넘기고 본문 없이 200 이다")
    void 읽음() {
        ResponseEntity<CommonApiResponse<Void>> response = controller.read(principal, NOTIFICATION);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody().getData()).isNull();
        verify(notificationService).read(ACCOUNT, NOTIFICATION);
    }

    @Test
    @DisplayName("모두 읽음은 내 계정을 넘기고 본문 없이 200 이다")
    void 모두_읽음() {
        ResponseEntity<CommonApiResponse<Void>> response = controller.readAll(principal);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody().getData()).isNull();
        verify(notificationService).readAll(ACCOUNT);
    }

    @Test
    @DisplayName("안 읽은 수는 이름 붙은 칸에 담겨 나간다")
    void 안_읽은_수() {
        when(notificationService.countUnread(ACCOUNT)).thenReturn(new UnreadCountOutput(4L));

        ResponseEntity<CommonApiResponse<UnreadCountOutput>> response = controller.countUnread(principal);

        assertThat(response.getBody().getData().unreadCount()).isEqualTo(4L);
    }

    @Test
    @DisplayName("설정 수정은 보낸 칸만 입력으로 넘긴다")
    void 설정_수정() {
        when(notificationSettingService.update(eq(ACCOUNT), inputCaptor.capture()))
                .thenReturn(new NotificationSettingOutput(false, true));

        ResponseEntity<CommonApiResponse<NotificationSettingOutput>> response =
                controller.updateSettings(principal, new NotificationSettingUpdateRequest(false, null));

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(inputCaptor.getValue().policyChanged()).isFalse();
        assertThat(inputCaptor.getValue().reportResolved()).isNull();
        assertThat(response.getBody().getData().policyChanged()).isFalse();
    }
}
