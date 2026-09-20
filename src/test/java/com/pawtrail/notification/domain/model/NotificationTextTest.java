package com.pawtrail.notification.domain.model;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * 알림 문구를 짓는 규칙을 검사합니다.
 */
class NotificationTextTest {

    @Test
    @DisplayName("판 1 은 새로 확인, 그 뒤로는 바뀜이다")
    void 판_제목() {
        assertThat(NotificationText.policyChanged(1, List.of("leashRequired")).title())
                .isEqualTo("동반 조건이 새로 확인되었습니다");
        assertThat(NotificationText.policyChanged(2, List.of("leashRequired")).title())
                .isEqualTo("동반 조건이 바뀌었습니다");
        assertThat(NotificationText.policyChanged(2, List.of("leashRequired")).body()).isEqualTo("목줄");
    }

    @Test
    @DisplayName("본문은 받은 차례대로 앞 셋까지 적고 나머지를 외 N개로 센다")
    void 앞_셋() {
        NotificationText text = NotificationText.policyChanged(3,
                List.of("maxWeightKg", "sizeRule", "leashRequired", "carrierRequired", "vaccineProof"));

        assertThat(text.body()).isEqualTo("체중 제한 · 크기 제한 · 목줄 외 2개");
        assertThat(NotificationText.policyChanged(3, List.of("maxWeightKg", "sizeRule")).body())
                .isEqualTo("체중 제한 · 크기 제한");
    }

    @Test
    @DisplayName("모르는 칸은 수에만 들어가고, 전부 모르면 조건 N개다")
    void 모르는_칸() {
        assertThat(NotificationText.policyChanged(2, List.of("maxWeightKg", "dogPark")).body())
                .isEqualTo("체중 제한 외 1개");
        assertThat(NotificationText.policyChanged(2, List.of("dogPark", "catCafe")).body())
                .isEqualTo("조건 2개");
    }

    @Test
    @DisplayName("제보 결과는 장소 제보 승인 · 후기 신고 승인 · 반려 셋으로 갈리고 본문은 메모 그대로다")
    void 제보_문구_셋() {
        NotificationText accepted = NotificationText.reportResolved("INFO_WRONG", "ACCEPTED", "고쳤습니다").get();
        NotificationText review = NotificationText.reportResolved("REVIEW_ABUSE", "ACCEPTED", "지웠습니다").get();
        NotificationText rejected = NotificationText.reportResolved("REVIEW_ABUSE", "REJECTED", "문제없습니다").get();

        assertThat(accepted.title()).isEqualTo("제보하신 내용이 반영되었습니다");
        assertThat(review.title()).isEqualTo("신고하신 후기가 처리되었습니다");
        assertThat(rejected.title()).isEqualTo("제보하신 내용은 반영되지 않았습니다");
        assertThat(accepted.body()).isEqualTo("고쳤습니다");
    }

    @Test
    @DisplayName("모르는 유형의 승인은 장소 제보 문구이고, 모르는 결과는 알릴 말이 없다")
    void 모르는_값() {
        assertThat(NotificationText.reportResolved("NEW_TYPE", "ACCEPTED", "메모").get().title())
                .isEqualTo("제보하신 내용이 반영되었습니다");
        assertThat(NotificationText.reportResolved("INFO_WRONG", "PENDING", "메모").isPresent()).isFalse();
    }

    @Test
    @DisplayName("본문 폭을 넘는 메모는 앞 499자에 … 를 붙여 자르고, 두 칸짜리 문자를 반으로 가르지 않는다")
    void 긴_메모() {
        NotificationText cut = NotificationText.reportResolved("CLOSED", "ACCEPTED", "가".repeat(600)).get();
        String withEmoji = NotificationText.reportResolved(
                "CLOSED", "ACCEPTED", "가".repeat(498) + "\uD83D\uDE00" + "나".repeat(10)).get().body();

        assertThat(cut.title()).isEqualTo("제보하신 내용이 반영되었습니다");
        assertThat(cut.body().length()).isEqualTo(500);
        assertThat(cut.body().endsWith("…")).isTrue();
        assertThat(withEmoji).isEqualTo("가".repeat(498) + "…");
    }

    @Test
    @DisplayName("메모가 비어 오면 대체 문구로 둔다")
    void 빈_메모() {
        assertThat(NotificationText.reportResolved("CLOSED", "ACCEPTED", "  ").get().body())
                .isEqualTo("처리 결과를 확인해 주세요");
        assertThat(NotificationText.reportResolved("CLOSED", "REJECTED", null).get().body())
                .isEqualTo("처리 결과를 확인해 주세요");
    }
}
