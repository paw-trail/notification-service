package com.pawtrail.notification.infrastructure.message.kafka.consumer;

import com.pawtrail.common.message.EventEnvelope;
import com.pawtrail.common.message.inbox.InboxProcessor;
import com.pawtrail.notification.application.service.NotificationCreateService;
import com.pawtrail.notification.infrastructure.message.kafka.consumer.dto.ReportResolvedMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

/**
 * report.resolved 를 받아 제보한 사람에게 처리 결과 알림을 만듭니다.
 *
 * 한 건씩 받고 Inbox 로 한 번만 처리합니다. 다른 서비스를 부르지 않습니다.
 *
 * 예외를 잡지 않습니다. 실패하면 세 번 다시 시도한 뒤 report.resolved.dlq 로 보냅니다.
 * 메모 내용은 로그에 남기지 않습니다. 관리자가 사용자에게 쓴 글이라서입니다.
 *
 * 소비 그룹은 서비스 이름(notification-service)입니다. config 1계층이 정합니다.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class ReportResolvedConsumer {

    private static final String TOPIC = "report.resolved";

    private final InboxProcessor inboxProcessor;
    private final NotificationCreateService notificationCreateService;

    @KafkaListener(topics = TOPIC)
    public void consume(EventEnvelope<ReportResolvedMessage> envelope) {
        ReportResolvedMessage message = envelope.data();
        log.info("report.resolved 수신: eventId={}, reportId={}, accountId={}, reportType={}, status={}",
                envelope.eventId(), message.reportId(), message.accountId(), message.reportType(), message.status());

        inboxProcessor.processOnce(
                envelope.eventId(),
                TOPIC,
                () -> notificationCreateService.notifyReportResolved(
                        message.accountId(), message.placeId(), message.reportType(), message.status(), message.memo())
        );
    }
}
