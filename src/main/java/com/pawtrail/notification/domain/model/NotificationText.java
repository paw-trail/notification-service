package com.pawtrail.notification.domain.model;

import com.pawtrail.notification.domain.enums.ConditionField;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

/**
 * 알림 한 건의 문구(제목 · 본문)입니다. 종류마다 문구를 짓는 규칙을 여기 모읍니다.
 *
 * 말투는 ~습니다 입니다. 제보 결과 문구 셋이 먼저 그렇게 정해져 있었습니다.
 * 장소 이름은 넣지 않습니다. 목록을 열 때 place 에서 받아 채웁니다.
 *
 * @param title 제목입니다.
 * @param body  본문입니다.
 */
public record NotificationText(String title, String body) {

    // 조건 변경 — 판 1 은 조건이 처음 읽힌 것이라 "바뀌었다" 가 틀린 말이 됨
    //   장소가 먼저 생기고 조건이 나중에 읽혀서, 조건이 처음 생기기 전에 즐겨찾기가 있을 수 있음
    static final String POLICY_FIRST_TITLE = "동반 조건이 새로 확인되었습니다";
    static final String POLICY_CHANGED_TITLE = "동반 조건이 바뀌었습니다";

    // 제보 결과 — 반려는 유형과 무관하게 한 문구
    static final String REPORT_ACCEPTED_TITLE = "제보하신 내용이 반영되었습니다";
    static final String REVIEW_ACCEPTED_TITLE = "신고하신 후기가 처리되었습니다";
    static final String REPORT_REJECTED_TITLE = "제보하신 내용은 반영되지 않았습니다";

    // 처리 메모가 비어 올 때의 본문
    static final String MEMO_FALLBACK = "처리 결과를 확인해 주세요";

    // 본문에 이름을 적는 칸 수 — 넘치는 칸은 "외 N개" 로 셈
    static final int LISTED_FIELDS = 3;

    /**
     * 조건 변경 알림의 문구를 짓습니다.
     *
     * 판이 1 이면 "새로 확인되었습니다", 그 뒤로는 "바뀌었습니다" 입니다.
     * 본문은 바뀐 칸의 이름표를 받은 순서(조건 순서)대로 앞 셋까지 적고 나머지는 "외 N개" 로 셉니다.
     *   예) 체중 제한 · 크기 제한 · 목줄 외 2개
     * 모르는 칸 이름은 이름표가 없어 수에만 들어갑니다. 전부 모르는 칸이면 "조건 N개" 입니다.
     *
     * 바뀐 칸이 없는 변경은 알리지 않으므로 부르는 쪽이 먼저 거릅니다. 비어서 오면 버그라 예외를 던집니다.
     */
    public static NotificationText policyChanged(int policyVersion, List<String> changedFields) {
        Objects.requireNonNull(changedFields, "changedFields");
        if (changedFields.isEmpty()) {
            throw new IllegalArgumentException("바뀐 칸이 없으면 문구를 짓지 않습니다");
        }
        List<String> fields = changedFields.stream().distinct().toList();
        List<String> labels = fields.stream()
                .map(ConditionField::ofName)
                .flatMap(Optional::stream)
                .map(ConditionField::label)
                .toList();

        String title = policyVersion == 1 ? POLICY_FIRST_TITLE : POLICY_CHANGED_TITLE;
        return new NotificationText(title, fieldsBody(labels, fields.size()));
    }

    /**
     * 제보 결과 알림의 문구를 짓습니다.
     *
     * 제목은 결과와 유형으로 갈립니다.
     *   반려                          유형과 무관하게 "반영되지 않았습니다"
     *   후기 신고(REVIEW_ABUSE) 승인     "신고하신 후기가 처리되었습니다"
     *   그 밖의 승인                    "반영되었습니다" — 모르는 유형도 여기로 옴
     *
     * 유형 · 결과를 문자열로 받습니다. report 가 값을 늘려도 여기가 깨지지 않게 하려는 것입니다.
     * 승인도 반려도 아닌 결과는 알릴 말이 없어 빈 값을 돌려줍니다. report 는 그런 결과를 내보내지 않습니다.
     *
     * 본문은 관리자 메모 그대로입니다. 비어 오면 대체 문구로, 본문 폭을 넘으면 잘라서 둡니다.
     * report 가 메모를 필수 · 500자로 받아 둘 다 올 일은 없으나, 메모 하나 때문에 결과 알림을 잃지 않으려는 것입니다.
     * 넘친 채로 두면 알림을 만들 때 예외가 나 재시도해도 같고, 끝내 .dlq 로 갑니다.
     * 사용자에게 중요한 것은 반영됐는지(제목)입니다.
     */
    public static Optional<NotificationText> reportResolved(String reportType, String status, String memo) {
        String title;
        if ("REJECTED".equals(status)) {
            title = REPORT_REJECTED_TITLE;
        } else if ("ACCEPTED".equals(status)) {
            title = "REVIEW_ABUSE".equals(reportType) ? REVIEW_ACCEPTED_TITLE : REPORT_ACCEPTED_TITLE;
        } else {
            return Optional.empty();
        }
        String body = memo == null || memo.isBlank() ? MEMO_FALLBACK : fitBody(memo);
        return Optional.of(new NotificationText(title, body));
    }

    /**
     * 본문 폭을 넘는 메모를 자릅니다. 앞 499자에 "…" 을 붙여 잘린 것이 보이게 합니다.
     *
     * 자르는 자리가 두 칸짜리 문자(이모지 같은 서로게이트 쌍)의 한가운데면 그 앞에서 자릅니다.
     * 반쪽만 남으면 깨진 글자가 됩니다.
     */
    static String fitBody(String memo) {
        if (memo.length() <= Notification.BODY_MAX) {
            return memo;
        }
        int end = Notification.BODY_MAX - 1;
        if (Character.isHighSurrogate(memo.charAt(end - 1))) {
            end--;
        }
        return memo.substring(0, end) + "…";
    }

    private static String fieldsBody(List<String> labels, int total) {
        if (labels.isEmpty()) {
            return "조건 " + total + "개";
        }
        List<String> listed = labels.subList(0, Math.min(LISTED_FIELDS, labels.size()));
        int rest = total - listed.size();
        String body = String.join(" · ", listed);
        return rest > 0 ? body + " 외 " + rest + "개" : body;
    }
}
