package com.pawtrail.notification.infrastructure.message.kafka.consumer;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import com.pawtrail.common.message.EventEnvelope;
import com.pawtrail.common.message.inbox.InboxProcessor;
import com.pawtrail.notification.application.service.NotificationCreateService;
import com.pawtrail.notification.infrastructure.message.kafka.consumer.dto.ReportResolvedMessage;
import java.time.LocalDateTime;
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
 * 받은 이벤트를 Inbox 에 넘기고, Inbox 가 돌릴 때 비로소 알림을 만드는지 검사합니다.
 */
@ExtendWith(MockitoExtension.class)
class ReportResolvedConsumerTest {

    private static final UUID EVENT = UUID.fromString("01999999-0000-7000-8000-0000000000e2");
    private static final UUID REPORT = UUID.fromString("01999999-0000-7000-8000-000000000301");
    private static final UUID ACCOUNT = UUID.fromString("01999999-0000-7000-8000-000000000001");
    private static final UUID PLACE = UUID.fromString("01999999-0000-7000-8000-00000000aaaa");

    @Mock
    private InboxProcessor inboxProcessor;

    @Mock
    private NotificationCreateService notificationCreateService;

    @InjectMocks
    private ReportResolvedConsumer consumer;

    @Captor
    private ArgumentCaptor<Runnable> actionCaptor;

    @Test
    @DisplayName("이벤트 식별자와 토픽으로 Inbox 에 넘기고, Inbox 가 돌리면 받는 사람 · 장소 · 유형 · 결과 · 메모를 넘긴다")
    void 넘긴다() {
        EventEnvelope<ReportResolvedMessage> envelope = new EventEnvelope<>(
                EVENT, "report.resolved", LocalDateTime.now(), "Report", REPORT.toString(),
                new ReportResolvedMessage(REPORT, ACCOUNT, "CLOSED", "ACCEPTED", "폐업을 확인했습니다", PLACE));

        consumer.consume(envelope);

        verify(inboxProcessor).processOnce(eq(EVENT), eq("report.resolved"), actionCaptor.capture());
        verify(notificationCreateService, never()).notifyReportResolved(any(), any(), any(), any(), any());

        actionCaptor.getValue().run();

        verify(notificationCreateService)
                .notifyReportResolved(ACCOUNT, PLACE, "CLOSED", "ACCEPTED", "폐업을 확인했습니다");
    }
}
