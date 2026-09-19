package com.pawtrail.notification.infrastructure.message.kafka.consumer;

import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import com.pawtrail.common.message.EventEnvelope;
import com.pawtrail.common.message.inbox.InboxProcessor;
import com.pawtrail.notification.application.service.AccountWithdrawnService;
import com.pawtrail.notification.infrastructure.message.kafka.consumer.dto.AccountWithdrawnMessage;
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
 * 받은 이벤트를 Inbox 에 넘기고, Inbox 가 돌릴 때 비로소 정리하는지 검사합니다.
 */
@ExtendWith(MockitoExtension.class)
class AccountWithdrawnConsumerTest {

    private static final UUID EVENT = UUID.fromString("01999999-0000-7000-8000-0000000000e3");
    private static final UUID ACCOUNT = UUID.fromString("01999999-0000-7000-8000-000000000001");

    @Mock
    private InboxProcessor inboxProcessor;

    @Mock
    private AccountWithdrawnService accountWithdrawnService;

    @InjectMocks
    private AccountWithdrawnConsumer consumer;

    @Captor
    private ArgumentCaptor<Runnable> actionCaptor;

    @Test
    @DisplayName("이벤트 식별자와 토픽으로 Inbox 에 넘기고, Inbox 가 돌리면 그 계정을 정리한다")
    void 넘긴다() {
        EventEnvelope<AccountWithdrawnMessage> envelope = new EventEnvelope<>(
                EVENT, "account.withdrawn", LocalDateTime.now(), "Account", ACCOUNT.toString(),
                new AccountWithdrawnMessage(ACCOUNT));

        consumer.consume(envelope);

        verify(inboxProcessor).processOnce(eq(EVENT), eq("account.withdrawn"), actionCaptor.capture());
        verify(accountWithdrawnService, never()).withdraw(ACCOUNT);

        actionCaptor.getValue().run();

        verify(accountWithdrawnService).withdraw(ACCOUNT);
    }
}
