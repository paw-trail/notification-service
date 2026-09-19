package com.pawtrail.notification.application.dto.output;

/**
 * 안 읽은 알림 수입니다. 헤더 벨의 숫자입니다.
 *
 * 숫자 하나를 이름 붙인 칸에 담아 내보냅니다.
 * 종류별 수처럼 칸이 늘어날 때 응답 모양이 바뀌지 않게 하려는 것입니다.
 *
 * @param unreadCount 안 읽은 알림 수입니다.
 */
public record UnreadCountOutput(long unreadCount) {
}
