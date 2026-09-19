package com.pawtrail.notification.infrastructure.message.kafka.consumer.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import java.util.List;
import java.util.UUID;

/**
 * policy.changed 의 본문(data)입니다. policy 가 보냅니다.
 *
 * 충돌 여부(hasConflict)는 받지 않습니다. 알림 문구에 넣지 않고, 충돌은 장소 상세의 배지가 보여 줍니다.
 * policy 가 칸을 더해도 깨지지 않게 모르는 칸은 무시합니다.
 *
 * @param placeId       조건이 바뀐 장소입니다.
 * @param policyVersion 바뀐 뒤의 판입니다. 1 이면 조건이 처음 읽힌 것입니다.
 * @param changedFields 값이 바뀐 조건 칸 이름입니다. 조건 순서이며 비어 있을 수 있습니다.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public record PolicyChangedMessage(UUID placeId, int policyVersion, List<String> changedFields) {
}
