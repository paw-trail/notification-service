package com.pawtrail.notification.infrastructure.message.kafka.consumer;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import com.pawtrail.common.message.EventEnvelope;
import com.pawtrail.common.message.inbox.InboxProcessor;
import com.pawtrail.notification.application.service.NotificationCreateService;
import com.pawtrail.notification.infrastructure.message.kafka.consumer.dto.PolicyChangedMessage;
import java.time.LocalDateTime;
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

/**
 * 받은 이벤트를 Inbox 에 넘기고, Inbox 가 돌릴 때 비로소 알림을 만드는지 검사합니다.
 *
 * 같은 이벤트를 두 번 거르는 일은 Inbox 가 하므로 여기서는 넘기는 값만 봅니다.
 */
@ExtendWith(MockitoExtension.class)
class PolicyChangedConsumerTest {

    private static final UUID EVENT = UUID.fromString("01999999-0000-7000-8000-0000000000e1");
    private static final UUID PLACE = UUID.fromString("01999999-0000-7000-8000-00000000aaaa");

    @Mock
    private InboxProcessor inboxProcessor;

    @Mock
    private NotificationCreateService notificationCreateService;

    @InjectMocks
    private PolicyChangedConsumer consumer;

    @Captor
    private ArgumentCaptor<Runnable> actionCaptor;

    @Test
    @DisplayName("이벤트 식별자와 토픽으로 Inbox 에 넘기고, Inbox 가 돌리면 장소 · 판 · 칸을 그대로 넘긴다")
    void 넘긴다() {
        EventEnvelope<PolicyChangedMessage> envelope = new EventEnvelope<>(
                EVENT, "policy.changed", LocalDateTime.now(), "Policy", PLACE.toString(),
                new PolicyChangedMessage(PLACE, 2, List.of("leashRequired")));

        consumer.consume(envelope);

        verify(inboxProcessor).processOnce(eq(EVENT), eq("policy.changed"), actionCaptor.capture());
        verify(notificationCreateService, never()).notifyPolicyChanged(any(), anyInt(), any());

        actionCaptor.getValue().run();

        verify(notificationCreateService).notifyPolicyChanged(PLACE, 2, List.of("leashRequired"));
    }
}
