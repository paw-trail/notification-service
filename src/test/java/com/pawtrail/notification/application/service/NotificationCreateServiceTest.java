package com.pawtrail.notification.application.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.pawtrail.notification.domain.enums.NotifType;
import com.pawtrail.notification.domain.model.Notification;
import com.pawtrail.notification.domain.model.NotificationSetting;
import com.pawtrail.notification.domain.provider.UserProvider;
import com.pawtrail.notification.domain.provider.dto.FavoritePage;
import com.pawtrail.notification.domain.repository.NotificationRepository;
import com.pawtrail.notification.domain.repository.NotificationSettingRepository;
import java.util.List;
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
 * 이벤트로 알림을 만드는 규칙을 검사합니다. 저장소 둘과 user 는 흉내 냅니다.
 */
@ExtendWith(MockitoExtension.class)
class NotificationCreateServiceTest {

    private static final UUID A = UUID.fromString("01999999-0000-7000-8000-00000000000a");
    private static final UUID B = UUID.fromString("01999999-0000-7000-8000-00000000000b");
    private static final UUID C = UUID.fromString("01999999-0000-7000-8000-00000000000c");
    private static final UUID D = UUID.fromString("01999999-0000-7000-8000-00000000000d");
    private static final UUID PLACE = UUID.fromString("01999999-0000-7000-8000-00000000aaaa");

    @Mock
    private NotificationRepository notificationRepository;

    @Mock
    private NotificationSettingRepository notificationSettingRepository;

    @Mock
    private UserProvider userProvider;

    @InjectMocks
    private NotificationCreateService service;

    @Captor
    private ArgumentCaptor<List<Notification>> notificationsCaptor;

    @Captor
    private ArgumentCaptor<Notification> notificationCaptor;

    @Test
    @DisplayName("바뀐 칸이 없으면 명단도 부르지 않고 아무것도 안 만든다")
    void 바뀐_칸_없음() {
        service.notifyPolicyChanged(PLACE, 3, List.of());

        verifyNoInteractions(userProvider, notificationRepository, notificationSettingRepository);
    }

    @Test
    @DisplayName("명단을 끝 쪽까지 받고, 끈 사람과 탈퇴 표시를 건너뛴 뒤 한 번에 갈아 끼운다")
    void 두_쪽_거르기() {
        NotificationSetting off = NotificationSetting.defaults(B);
        off.change(false, null);
        NotificationSetting marked = NotificationSetting.defaults(C);
        marked.delete("SYSTEM");
        when(userProvider.findFavoriteAccountIds(PLACE, 0)).thenReturn(new FavoritePage(List.of(A, B, C), false));
        when(userProvider.findFavoriteAccountIds(PLACE, 1)).thenReturn(new FavoritePage(List.of(D), true));
        when(notificationSettingRepository.findAllByAccountIds(List.of(A, B, C))).thenReturn(List.of(off, marked));
        when(notificationSettingRepository.findAllByAccountIds(List.of(D))).thenReturn(List.of());
        when(notificationRepository.deleteUnreadPolicyChanged(List.of(A, D), PLACE)).thenReturn(1);

        service.notifyPolicyChanged(PLACE, 2,
                List.of("maxWeightKg", "sizeRule", "leashRequired", "carrierRequired"));

        verify(notificationRepository).saveAll(notificationsCaptor.capture());
        List<Notification> saved = notificationsCaptor.getValue();
        assertThat(saved).extracting(Notification::getAccountId).containsExactly(A, D);
        assertThat(saved.get(0).getNotifType()).isEqualTo(NotifType.POLICY_CHANGED);
        assertThat(saved.get(0).getPlaceId()).isEqualTo(PLACE);
        assertThat(saved.get(0).getTitle()).isEqualTo("동반 조건이 바뀌었습니다");
        assertThat(saved.get(0).getBody()).isEqualTo("체중 제한 · 크기 제한 · 목줄 외 1개");
    }

    @Test
    @DisplayName("중간 쪽에서 user 가 멈추면 아무것도 쓰지 않고 예외를 올린다")
    void user_실패() {
        when(userProvider.findFavoriteAccountIds(PLACE, 0)).thenReturn(new FavoritePage(List.of(A), false));
        when(notificationSettingRepository.findAllByAccountIds(List.of(A))).thenReturn(List.of());
        when(userProvider.findFavoriteAccountIds(PLACE, 1)).thenThrow(new IllegalStateException("user 가 멈춤"));

        assertThatThrownBy(() -> service.notifyPolicyChanged(PLACE, 2, List.of("leashRequired")))
                .isInstanceOf(IllegalStateException.class);
        verify(notificationRepository, never()).deleteUnreadPolicyChanged(any(), any());
        verify(notificationRepository, never()).saveAll(any());
    }

    @Test
    @DisplayName("제보 결과는 설정 행이 없으면 만들고, 후기 신고 승인 문구와 메모가 담긴다")
    void 제보_결과_만들기() {
        when(notificationSettingRepository.findByAccountId(A)).thenReturn(Optional.empty());
        when(notificationRepository.save(notificationCaptor.capture()))
                .thenAnswer(invocation -> invocation.getArgument(0));

        service.notifyReportResolved(A, PLACE, "REVIEW_ABUSE", "ACCEPTED", "욕설이 있어 지웠습니다");

        Notification saved = notificationCaptor.getValue();
        assertThat(saved.getAccountId()).isEqualTo(A);
        assertThat(saved.getNotifType()).isEqualTo(NotifType.REPORT_RESOLVED);
        assertThat(saved.getPlaceId()).isEqualTo(PLACE);
        assertThat(saved.getTitle()).isEqualTo("신고하신 후기가 처리되었습니다");
        assertThat(saved.getBody()).isEqualTo("욕설이 있어 지웠습니다");
    }

    @Test
    @DisplayName("제보 결과를 끈 사람과 탈퇴 표시가 찍힌 사람에게는 만들지 않는다")
    void 제보_결과_거름() {
        NotificationSetting off = NotificationSetting.defaults(A);
        off.change(null, false);
        NotificationSetting marked = NotificationSetting.defaults(B);
        marked.delete("SYSTEM");
        when(notificationSettingRepository.findByAccountId(A)).thenReturn(Optional.of(off));
        when(notificationSettingRepository.findByAccountId(B)).thenReturn(Optional.of(marked));

        service.notifyReportResolved(A, PLACE, "INFO_WRONG", "ACCEPTED", "고쳤습니다");
        service.notifyReportResolved(B, PLACE, "INFO_WRONG", "REJECTED", "기존 정보가 맞습니다");

        verify(notificationRepository, never()).save(any());
    }

    @Test
    @DisplayName("알릴 말이 없는 결과면 설정도 안 보고 만들지 않는다")
    void 제보_결과_모르는_결과() {
        service.notifyReportResolved(A, PLACE, "INFO_WRONG", "PENDING", "메모");

        verifyNoInteractions(notificationSettingRepository, notificationRepository);
    }
}
