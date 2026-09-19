package com.pawtrail.notification.infrastructure.persistence;

import static org.assertj.core.api.Assertions.assertThat;

import com.pawtrail.notification.IntegrationTestSupport;
import com.pawtrail.notification.domain.enums.NotifType;
import com.pawtrail.notification.domain.model.Notification;
import com.pawtrail.notification.domain.model.NotificationSetting;
import com.pawtrail.notification.domain.repository.NotificationRepository;
import com.pawtrail.notification.domain.repository.NotificationSettingRepository;
import com.pawtrail.notification.infrastructure.persistence.jpa.NotificationJpaRepository;
import com.pawtrail.notification.infrastructure.persistence.jpa.NotificationSettingJpaRepository;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

/**
 * V20 의 표와 저장소 조회를 실제 PostgreSQL 로 검사합니다.
 *
 * 트랜잭션을 걸지 않습니다. 저장마다 실제로 커밋해야 다른 연결의 조회가 그 행을 봅니다.
 * 대신 검사마다 표를 비웁니다.
 */
class NotificationRepositoryImplTest extends IntegrationTestSupport {

    private static final UUID ACCOUNT = UUID.fromString("01999999-0000-7000-8000-000000000001");
    private static final UUID OTHER_ACCOUNT = UUID.fromString("01999999-0000-7000-8000-000000000002");
    private static final UUID THIRD_ACCOUNT = UUID.fromString("01999999-0000-7000-8000-000000000003");
    private static final UUID PLACE = UUID.fromString("01999999-0000-7000-8000-00000000aaaa");
    private static final UUID OTHER_PLACE = UUID.fromString("01999999-0000-7000-8000-00000000cccc");

    @Autowired
    private NotificationRepository notificationRepository;

    @Autowired
    private NotificationSettingRepository notificationSettingRepository;

    @Autowired
    private NotificationJpaRepository notificationJpaRepository;

    @Autowired
    private NotificationSettingJpaRepository notificationSettingJpaRepository;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private PlatformTransactionManager transactionManager;

    @AfterEach
    void cleanUp() {
        notificationJpaRepository.deleteAll();
        notificationSettingJpaRepository.deleteAll();
    }

    @Test
    @DisplayName("목록은 최신순이고 같은 시각이면 식별자 순이며 남의 알림은 섞이지 않는다")
    void 목록_차례() {
        Notification oldest = notificationRepository.save(notification(ACCOUNT));
        Notification tiedA = notificationRepository.save(notification(ACCOUNT));
        Notification tiedB = notificationRepository.save(notification(ACCOUNT));
        notificationRepository.save(notification(OTHER_ACCOUNT));
        setCreatedAt(oldest.getId(), LocalDateTime.of(2026, 9, 20, 9, 0));
        setCreatedAt(tiedA.getId(), LocalDateTime.of(2026, 9, 20, 10, 0));
        setCreatedAt(tiedB.getId(), LocalDateTime.of(2026, 9, 20, 10, 0));

        // 같은 시각인 둘의 차례는 PostgreSQL 이 uuid 를 견주는 방식으로 정해지므로 DB 에 직접 물어 기대값을 만듦
        List<UUID> expected = jdbcTemplate.queryForList(
                "SELECT id FROM notification WHERE account_id = ? ORDER BY created_at DESC, id DESC",
                UUID.class, ACCOUNT);

        Page<Notification> first = notificationRepository.findByAccountId(ACCOUNT, 0, 2);
        Page<Notification> second = notificationRepository.findByAccountId(ACCOUNT, 1, 2);

        assertThat(first.getTotalElements()).isEqualTo(3L);
        assertThat(first.getContent()).extracting(Notification::getId)
                .containsExactly(expected.get(0), expected.get(1));
        assertThat(second.getContent()).extracting(Notification::getId)
                .containsExactly(expected.get(2));
        assertThat(expected.get(2)).isEqualTo(oldest.getId());
    }

    @Test
    @DisplayName("남의 알림은 식별자로도 찾지 못한다")
    void 남의_알림() {
        Notification mine = notificationRepository.save(notification(ACCOUNT));

        assertThat(notificationRepository.findByIdAndAccountId(mine.getId(), ACCOUNT).isPresent()).isTrue();
        assertThat(notificationRepository.findByIdAndAccountId(mine.getId(), OTHER_ACCOUNT).isPresent()).isFalse();
    }

    @Test
    @DisplayName("안 읽은 알림만 세고 읽는다 — 읽은 것과 남의 것은 빠진다")
    void 안_읽은_것() {
        Notification read = notificationRepository.save(notification(ACCOUNT));
        notificationRepository.save(notification(ACCOUNT));
        notificationRepository.save(notification(ACCOUNT));
        notificationRepository.save(notification(OTHER_ACCOUNT));
        jdbcTemplate.update("UPDATE notification SET read_at = now() WHERE id = ?", read.getId());

        assertThat(notificationRepository.countUnreadByAccountId(ACCOUNT)).isEqualTo(2L);
        assertThat(notificationRepository.findUnreadByAccountId(ACCOUNT)).hasSize(2);
    }

    @Test
    @DisplayName("설정은 탈퇴 표시가 찍힌 행도 읽는다 — 늦게 온 알림거리를 거르는 데 필요함")
    void 설정_표시_행() {
        notificationSettingRepository.save(NotificationSetting.defaults(ACCOUNT));
        jdbcTemplate.update(
                "UPDATE notification_setting SET deleted_at = now(), deleted_by = 'SYSTEM' WHERE account_id = ?",
                ACCOUNT);

        Optional<NotificationSetting> found = notificationSettingRepository.findByAccountId(ACCOUNT);

        assertThat(found.isPresent()).isTrue();
        assertThat(found.get().isDeleted()).isTrue();
        assertThat(notificationSettingRepository.findByAccountId(OTHER_ACCOUNT).isPresent()).isFalse();
    }

    @Test
    @DisplayName("갈아 끼우기는 그 사람들의 그 장소 · 안 읽은 조건 알림만 지운다")
    void 갈아_끼우기_지우기() {
        Notification target = notificationRepository.save(notification(ACCOUNT));
        Notification read = notificationRepository.save(notification(ACCOUNT));
        notificationRepository.save(Notification.create(
                ACCOUNT, NotifType.REPORT_RESOLVED, PLACE, "제보하신 내용이 반영되었습니다", "고쳤습니다"));
        notificationRepository.save(Notification.create(
                ACCOUNT, NotifType.POLICY_CHANGED, OTHER_PLACE, "동반 조건이 바뀌었습니다", "목줄"));
        notificationRepository.save(notification(OTHER_ACCOUNT));
        jdbcTemplate.update("UPDATE notification SET read_at = now() WHERE id = ?", read.getId());

        // 지우기 문장은 쓰기 트랜잭션 안에서만 돎 — 실제로는 Inbox 가 여는 트랜잭션
        Integer deleted = new TransactionTemplate(transactionManager)
                .execute(status -> notificationRepository.deleteUnreadPolicyChanged(List.of(ACCOUNT), PLACE));

        List<UUID> left = jdbcTemplate.queryForList("SELECT id FROM notification", UUID.class);
        assertThat(deleted).isEqualTo(1);
        assertThat(left).hasSize(4);
        assertThat(left.contains(target.getId())).isFalse();
    }

    @Test
    @DisplayName("설정 여럿 읽기는 탈퇴 표시 행까지 돌려주고 행이 없는 사람은 빠진다")
    void 설정_여럿() {
        notificationSettingRepository.save(NotificationSetting.defaults(ACCOUNT));
        notificationSettingRepository.save(NotificationSetting.defaults(OTHER_ACCOUNT));
        jdbcTemplate.update(
                "UPDATE notification_setting SET deleted_at = now(), deleted_by = 'SYSTEM' WHERE account_id = ?",
                OTHER_ACCOUNT);

        List<NotificationSetting> found = notificationSettingRepository.findAllByAccountIds(
                List.of(ACCOUNT, OTHER_ACCOUNT, THIRD_ACCOUNT));

        assertThat(found).hasSize(2);
        assertThat(found.stream().filter(NotificationSetting::isDeleted).count()).isEqualTo(1L);
    }

    private static Notification notification(UUID accountId) {
        return Notification.create(accountId, NotifType.POLICY_CHANGED, PLACE, "동반 조건이 바뀌었습니다", "체중 제한");
    }

    private void setCreatedAt(UUID id, LocalDateTime createdAt) {
        jdbcTemplate.update("UPDATE notification SET created_at = ? WHERE id = ?", createdAt, id);
    }
}
