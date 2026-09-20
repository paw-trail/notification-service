package com.pawtrail.notification.infrastructure.message.kafka.consumer;

import com.pawtrail.common.message.EventEnvelope;
import com.pawtrail.common.message.inbox.InboxProcessor;
import com.pawtrail.notification.application.service.NotificationCreateService;
import com.pawtrail.notification.infrastructure.message.kafka.consumer.dto.PolicyChangedMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

/**
 * policy.changed 를 받아 그 장소를 즐겨찾기한 사람들에게 조건 변경 알림을 만듭니다.
 *
 * 한 건씩 받습니다. 같은 이벤트가 두 번 와도 Inbox 가 이벤트 식별자로 걸러 한 번만 만듭니다.
 * 카프카는 최소 한 번 전달이라 재시도 · 재기동 때 같은 메시지가 다시 올 수 있습니다.
 *
 * 예외를 잡지 않습니다.
 * user 를 못 불러 명단을 모르면 공통 오류 처리기가 1 · 2 · 4초 간격으로 세 번 다시 시도하고,
 * 끝내 안 되면 policy.changed.dlq 로 보냅니다. 그 이벤트의 알림은 만들어지지 않습니다.
 * 조건 변경 알림은 놓쳐도 사실이 틀어지지 않습니다. 장소 상세와 판정은 늘 지금 조건을 보여 줍니다.
 *
 * 소비 그룹은 서비스 이름(notification-service)입니다. config 1계층이 정합니다.
 * 그래서 이 토픽을 받는 다른 서비스와 별개로 모든 메시지를 받습니다.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class PolicyChangedConsumer {

    private static final String TOPIC = "policy.changed";

    private final InboxProcessor inboxProcessor;
    private final NotificationCreateService notificationCreateService;

    @KafkaListener(topics = TOPIC)
    public void consume(EventEnvelope<PolicyChangedMessage> envelope) {
        PolicyChangedMessage message = envelope.data();
        log.info("policy.changed 수신: eventId={}, placeId={}, policyVersion={}, changedFields={}",
                envelope.eventId(), message.placeId(), message.policyVersion(), message.changedFields());

        inboxProcessor.processOnce(
                envelope.eventId(),
                TOPIC,
                () -> notificationCreateService.notifyPolicyChanged(
                        message.placeId(), message.policyVersion(), message.changedFields())
        );
    }
}
