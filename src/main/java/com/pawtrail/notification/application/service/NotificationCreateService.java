package com.pawtrail.notification.application.service;

import com.pawtrail.notification.domain.enums.NotifType;
import com.pawtrail.notification.domain.model.Notification;
import com.pawtrail.notification.domain.model.NotificationSetting;
import com.pawtrail.notification.domain.model.NotificationText;
import com.pawtrail.notification.domain.provider.UserProvider;
import com.pawtrail.notification.domain.provider.dto.FavoritePage;
import com.pawtrail.notification.domain.repository.NotificationRepository;
import com.pawtrail.notification.domain.repository.NotificationSettingRepository;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

/**
 * 받은 이벤트로 알림을 만드는 일을 맡습니다.
 *
 * 사용자 요청이 아니라 이벤트 소비자가 부릅니다.
 * 트랜잭션을 여기서 열지 않습니다. 소비자가 Inbox 로 감싸 부르고 Inbox 가 엽니다.
 * 그래서 이벤트 하나의 알림은 전부 들어가거나 하나도 안 들어가고, 처리 기록과 함께 남습니다.
 *
 * 받을 사람을 거르는 규칙은 두 종류가 같습니다.
 *   설정 행이 없음           받음 (행이 없으면 전부 받는 것으로 봄)
 *   그 종류를 끔             건너뜀
 *   탈퇴 표시가 찍힘         건너뜀 — 탈퇴보다 늦게 도착한 알림거리
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class NotificationCreateService {

    // 지우기 · 쓰기를 한 번에 몇 명씩 할지 — 명단 한 쪽과 같게 둠
    //   한 문장에 식별자를 너무 많이 실으면 JDBC 의 바인딩 수 한도(32767)에 걸림
    static final int WRITE_CHUNK = 100;

    private final NotificationRepository notificationRepository;
    private final NotificationSettingRepository notificationSettingRepository;
    private final UserProvider userProvider;

    /**
     * 장소의 조건이 바뀌었을 때 그 장소를 즐겨찾기한 사람들에게 알립니다. policy.changed 를 받으면 부릅니다.
     *
     * 차례
     *   ① 바뀐 칸이 없으면 끝 — 충돌 여부 · 정정 출처 · 근거만 바뀐 변경은 사람에게 알릴 것이 없음
     *   ② user 에서 명단을 한 쪽씩 끝까지 받으며, 쪽마다 수신 설정을 한 번 읽어 받을 사람을 거름
     *      못 받으면 예외가 올라가 이 이벤트를 처음부터 다시 처리함
     *   ③ 명단을 다 모은 뒤 그 사람들에게 그 장소로 나간 안 읽은 조건 알림을 지우고 새 알림을 씀
     *
     * ③ 에서 지우는 이유는 같은 장소의 조건이 짧은 사이 거듭 바뀔 때 알림이 쌓이지 않게 하려는 것입니다.
     * 적재를 다시 돌리면 같은 장소에 판이 연달아 오르는데, 그때마다 한 줄씩 쌓이면 목록이 한 장소로 도배됩니다.
     * 앞 변경의 칸 이름은 덮이지만, 알림을 누르면 가는 장소 상세가 늘 지금 조건 전부를 보여 줍니다.
     * 읽은 알림은 지우지 않습니다. 이미 본 것이라 새 알림과 겹쳐 보이지 않습니다.
     *
     * 명단을 다 모은 뒤에 씁니다. 중간 쪽에서 user 가 멈추면 아무것도 안 쓴 채 다시 시도하게 됩니다.
     */
    public void notifyPolicyChanged(UUID placeId, int policyVersion, List<String> changedFields) {
        if (changedFields == null || changedFields.isEmpty()) {
            log.info("바뀐 조건 칸이 없어 알리지 않습니다: placeId={}, policyVersion={}", placeId, policyVersion);
            return;
        }
        NotificationText text = NotificationText.policyChanged(policyVersion, changedFields);

        List<UUID> recipients = new ArrayList<>();
        int favorites = 0;
        int page = 0;
        while (true) {
            FavoritePage favoritePage = userProvider.findFavoriteAccountIds(placeId, page);
            favorites += favoritePage.accountIds().size();
            recipients.addAll(receiversOf(favoritePage.accountIds(), NotifType.POLICY_CHANGED));
            if (favoritePage.last()) {
                break;
            }
            page++;
        }

        int replaced = 0;
        for (int from = 0; from < recipients.size(); from += WRITE_CHUNK) {
            List<UUID> chunk = recipients.subList(from, Math.min(from + WRITE_CHUNK, recipients.size()));
            replaced += notificationRepository.deleteUnreadPolicyChanged(chunk, placeId);
            notificationRepository.saveAll(chunk.stream()
                    .map(accountId -> Notification.create(
                            accountId, NotifType.POLICY_CHANGED, placeId, text.title(), text.body()))
                    .toList());
        }

        log.info("조건 변경 알림을 만들었습니다: placeId={}, policyVersion={}, 즐겨찾기 {}명, 알림 {}건, 갈아 끼움 {}건",
                placeId, policyVersion, favorites, recipients.size(), replaced);
    }

    /**
     * 관리자가 제보를 처리했을 때 제보한 사람에게 알립니다. report.resolved 를 받으면 부릅니다.
     *
     * 다른 서비스를 부르지 않습니다. 받는 사람과 장소가 이벤트에 다 실려 옵니다.
     *
     * 알릴 말이 없는 결과(승인도 반려도 아님)면 알리지 않고 경고를 남깁니다.
     * 메모가 비어 오면 대체 문구로 알리고 경고를 남깁니다. 둘 다 report 쪽 이상의 신호입니다.
     */
    public void notifyReportResolved(UUID accountId, UUID placeId, String reportType, String status, String memo) {
        Optional<NotificationText> text = NotificationText.reportResolved(reportType, status, memo);
        if (text.isEmpty()) {
            log.warn("알릴 말이 없는 처리 결과라 알리지 않습니다: accountId={}, reportType={}, status={}",
                    accountId, reportType, status);
            return;
        }
        if (memo == null || memo.isBlank()) {
            log.warn("처리 메모가 비어 있어 대체 문구로 알립니다: accountId={}, reportType={}, status={}",
                    accountId, reportType, status);
        }

        Optional<NotificationSetting> setting = notificationSettingRepository.findByAccountId(accountId);
        if (setting.isPresent() && !setting.get().receives(NotifType.REPORT_RESOLVED)) {
            log.info("제보 결과 알림을 만들지 않습니다: accountId={}, reason={}",
                    accountId, setting.get().isDeleted() ? "탈퇴 표시" : "수신 끔");
            return;
        }

        Notification saved = notificationRepository.save(Notification.create(
                accountId, NotifType.REPORT_RESOLVED, placeId, text.get().title(), text.get().body()));
        log.info("제보 결과 알림을 만들었습니다: notificationId={}, accountId={}, reportType={}, status={}",
                saved.getId(), accountId, reportType, status);
    }

    /**
     * 명단 한 쪽에서 그 종류의 알림을 받을 사람만 남깁니다. 설정 표는 한 번만 읽습니다.
     */
    private List<UUID> receiversOf(List<UUID> accountIds, NotifType notifType) {
        if (accountIds.isEmpty()) {
            return List.of();
        }
        Map<UUID, NotificationSetting> settings = notificationSettingRepository.findAllByAccountIds(accountIds)
                .stream()
                .collect(Collectors.toMap(NotificationSetting::getAccountId, Function.identity()));

        return accountIds.stream()
                .filter(accountId -> {
                    NotificationSetting setting = settings.get(accountId);
                    return setting == null || setting.receives(notifType);
                })
                .toList();
    }
}
