package com.pawtrail.notification.domain.enums;

/**
 * 알림 종류입니다.
 *
 * notification.notif_type 에 이름 그대로 담기고, 화면이 아이콘과 문구를 가르는 데 씁니다.
 * 상수 이름이 곧 화면과의 약속이라 바꾸지 않습니다.
 *
 * 수신 설정의 칸도 종류마다 하나씩 있습니다.
 * 종류를 더하면 notification_setting 에도 칸을 더해야 합니다.
 */
public enum NotifType {

    // 즐겨찾기한 장소의 동반 조건이 바뀜 — policy.changed 를 받아 만듦
    POLICY_CHANGED,

    // 올린 제보를 관리자가 처리함 — report.resolved 를 받아 만듦
    REPORT_RESOLVED
}
