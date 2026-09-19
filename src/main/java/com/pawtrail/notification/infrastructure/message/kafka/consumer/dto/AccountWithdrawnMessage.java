package com.pawtrail.notification.infrastructure.message.kafka.consumer.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import java.util.UUID;

/**
 * account.withdrawn 의 본문(data)입니다. auth 가 보냅니다.
 *
 * 계정 식별자 하나만 씁니다. auth 가 칸을 더해도 깨지지 않게 모르는 칸은 무시합니다.
 *
 * @param accountId 탈퇴한 계정입니다.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public record AccountWithdrawnMessage(UUID accountId) {
}
