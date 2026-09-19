package com.pawtrail.notification.infrastructure.message.kafka.consumer;

import com.pawtrail.common.message.EventEnvelope;
import com.pawtrail.common.message.inbox.InboxProcessor;
import com.pawtrail.notification.application.service.AccountWithdrawnService;
import com.pawtrail.notification.infrastructure.message.kafka.consumer.dto.AccountWithdrawnMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

/**
 * account.withdrawn 을 받아 탈퇴한 계정의 알림을 지우고 설정에 탈퇴 표시를 남깁니다.
 * report · pet 의 소비자와 같은 모양입니다.
 *
 * 같은 이벤트가 두 번 와도 한 번만 처리합니다. Inbox 가 이벤트 식별자로 거릅니다.
 *
 * 예외를 잡지 않습니다.
 * 지우다 실패하면 공통 오류 처리기가 1 · 2 · 4초 간격으로 세 번 다시 시도하고,
 * 끝내 안 되면 account.withdrawn.dlq 로 보냅니다. 여기서 잡으면 실패가 조용히 묻힙니다.
 *
 * 소비 그룹은 서비스 이름(notification-service)입니다. config 1계층이 정합니다.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class AccountWithdrawnConsumer {

    private static final String TOPIC = "account.withdrawn";

    private final InboxProcessor inboxProcessor;
    private final AccountWithdrawnService accountWithdrawnService;

    @KafkaListener(topics = TOPIC)
    public void consume(EventEnvelope<AccountWithdrawnMessage> envelope) {
        AccountWithdrawnMessage message = envelope.data();
        log.info("account.withdrawn 수신: eventId={}, accountId={}", envelope.eventId(), message.accountId());

        inboxProcessor.processOnce(
                envelope.eventId(),
                TOPIC,
                () -> accountWithdrawnService.withdraw(message.accountId())
        );
    }
}
