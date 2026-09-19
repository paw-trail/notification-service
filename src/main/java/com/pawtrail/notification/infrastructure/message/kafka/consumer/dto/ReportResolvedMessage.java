package com.pawtrail.notification.infrastructure.message.kafka.consumer.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import java.util.UUID;

/**
 * report.resolved 의 본문(data)입니다. report 가 보냅니다.
 *
 * 유형 · 결과를 열거형이 아니라 문자열로 받습니다.
 * 열거형으로 받으면 report 가 값을 늘리는 순간 읽기부터 실패해 .dlq 로 갑니다.
 * 모르는 값을 어떻게 다룰지는 문구를 짓는 NotificationText 가 정합니다.
 *
 * @param reportId   처리한 제보입니다. 로그에만 씁니다.
 * @param accountId  제보한 사람입니다. 알림을 받을 사람입니다.
 * @param reportType 제보 유형입니다.
 * @param status     처리 결과입니다. ACCEPTED 또는 REJECTED 입니다.
 * @param memo       관리자 메모입니다. 알림 본문에 그대로 실립니다.
 * @param placeId    제보 대상 장소입니다. 후기 신고는 그 후기가 달린 장소입니다.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public record ReportResolvedMessage(UUID reportId,
                                    UUID accountId,
                                    String reportType,
                                    String status,
                                    String memo,
                                    UUID placeId) {
}
