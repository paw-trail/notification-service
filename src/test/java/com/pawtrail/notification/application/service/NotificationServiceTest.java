package com.pawtrail.notification.application.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.pawtrail.common.exception.CustomException;
import com.pawtrail.common.response.PageResponse;
import com.pawtrail.notification.application.dto.output.NotificationCardOutput;
import com.pawtrail.notification.domain.enums.NotifType;
import com.pawtrail.notification.domain.exception.NotificationErrorCode;
import com.pawtrail.notification.domain.model.Notification;
import com.pawtrail.notification.domain.provider.PlaceProvider;
import com.pawtrail.notification.domain.provider.dto.PlaceData;
import com.pawtrail.notification.domain.repository.NotificationRepository;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;

/**
 * 알림 목록 · 읽음 · 모두 읽음 · 안 읽은 수를 검사합니다. 저장소와 place 는 흉내 냅니다.
 */
@ExtendWith(MockitoExtension.class)
class NotificationServiceTest {

    private static final UUID ACCOUNT = UUID.fromString("01999999-0000-7000-8000-000000000001");
    private static final UUID PLACE = UUID.fromString("01999999-0000-7000-8000-00000000aaaa");
    private static final UUID GONE_PLACE = UUID.fromString("01999999-0000-7000-8000-00000000cccc");
    private static final UUID NOTIFICATION = UUID.fromString("01999999-0000-7000-8000-000000000101");

    @Mock
    private NotificationRepository notificationRepository;

    @Mock
    private PlaceProvider placeProvider;

    @InjectMocks
    private NotificationService notificationService;

    @Test
    @DisplayName("목록은 장소 이름을 채우고, place 가 모르는 장소는 이름만 비운다")
    void 목록_이름() {
        Notification known = policyChanged(PLACE);
        Notification gone = policyChanged(GONE_PLACE);
        when(notificationRepository.findByAccountId(ACCOUNT, 0, 20)).thenReturn(page(List.of(known, gone)));
        when(placeProvider.findByIds(List.of(PLACE, GONE_PLACE)))
                .thenReturn(Map.of(PLACE, new PlaceData(PLACE, "북악하늘길")));

        PageResponse<NotificationCardOutput> response = notificationService.getMine(ACCOUNT, 0, 20);

        assertThat(response.content()).extracting(NotificationCardOutput::placeName)
                .containsExactly("북악하늘길", null);
        assertThat(response.page().totalElements()).isEqualTo(2L);
    }

    @Test
    @DisplayName("place 를 못 부르면 이름 없이 목록을 그대로 낸다")
    void 목록_place_실패() {
        when(notificationRepository.findByAccountId(ACCOUNT, 0, 20)).thenReturn(page(List.of(policyChanged(PLACE))));
        when(placeProvider.findByIds(List.of(PLACE))).thenReturn(null);

        PageResponse<NotificationCardOutput> response = notificationService.getMine(ACCOUNT, 0, 20);

        assertThat(response.content()).hasSize(1);
        assertThat(response.content().get(0).placeName()).isNull();
        assertThat(response.content().get(0).title()).isEqualTo("동반 조건이 바뀌었습니다");
    }

    @Test
    @DisplayName("빈 쪽이면 place 를 부르지 않는다")
    void 목록_빈_쪽() {
        when(notificationRepository.findByAccountId(ACCOUNT, 3, 20))
                .thenReturn(new PageImpl<>(List.of(), PageRequest.of(3, 20), 0));

        PageResponse<NotificationCardOutput> response = notificationService.getMine(ACCOUNT, 3, 20);

        assertThat(response.content()).isEmpty();
        verify(placeProvider, never()).findByIds(any());
    }

    @Test
    @DisplayName("없거나 남의 알림을 읽으면 404 NOTIFICATION_NOT_FOUND")
    void 읽음_없음() {
        when(notificationRepository.findByIdAndAccountId(NOTIFICATION, ACCOUNT)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> notificationService.read(ACCOUNT, NOTIFICATION))
                .isInstanceOfSatisfying(CustomException.class,
                        e -> assertThat(e.getErrorCode()).isEqualTo(NotificationErrorCode.NOTIFICATION_NOT_FOUND));
    }

    @Test
    @DisplayName("읽음은 처음 한 번만 시각을 남기고, 이미 읽은 알림은 그대로 둔다")
    void 읽음_한_번() {
        Notification unread = policyChanged(PLACE);
        Notification alreadyRead = policyChanged(PLACE);
        LocalDateTime earlier = LocalDateTime.of(2026, 9, 1, 9, 0);
        alreadyRead.markRead(earlier);
        UUID other = UUID.fromString("01999999-0000-7000-8000-000000000102");
        when(notificationRepository.findByIdAndAccountId(NOTIFICATION, ACCOUNT)).thenReturn(Optional.of(unread));
        when(notificationRepository.findByIdAndAccountId(other, ACCOUNT)).thenReturn(Optional.of(alreadyRead));

        notificationService.read(ACCOUNT, NOTIFICATION);
        notificationService.read(ACCOUNT, other);

        assertThat(unread.getReadAt()).isNotNull();
        assertThat(alreadyRead.getReadAt()).isEqualTo(earlier);
    }

    @Test
    @DisplayName("모두 읽음은 안 읽은 알림을 하나씩 읽음으로 표시한다")
    void 모두_읽음() {
        Notification first = policyChanged(PLACE);
        Notification second = reportResolved(PLACE);
        when(notificationRepository.findUnreadByAccountId(ACCOUNT)).thenReturn(List.of(first, second));

        notificationService.readAll(ACCOUNT);

        assertThat(first.isRead()).isTrue();
        assertThat(second.isRead()).isTrue();
    }

    @Test
    @DisplayName("안 읽은 수는 저장소가 센 값을 그대로 담는다")
    void 안_읽은_수() {
        when(notificationRepository.countUnreadByAccountId(ACCOUNT)).thenReturn(3L);

        assertThat(notificationService.countUnread(ACCOUNT).unreadCount()).isEqualTo(3L);
    }

    private static Notification policyChanged(UUID placeId) {
        return Notification.create(ACCOUNT, NotifType.POLICY_CHANGED, placeId, "동반 조건이 바뀌었습니다", "체중 제한");
    }

    private static Notification reportResolved(UUID placeId) {
        return Notification.create(ACCOUNT, NotifType.REPORT_RESOLVED, placeId, "제보하신 내용이 반영되었습니다", "고쳤습니다");
    }

    private static Page<Notification> page(List<Notification> content) {
        return new PageImpl<>(content, PageRequest.of(0, 20), content.size());
    }
}
