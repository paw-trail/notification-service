package com.pawtrail.notification.presentation.controller;

import com.pawtrail.common.response.CommonApiResponse;
import com.pawtrail.common.response.PageResponse;
import com.pawtrail.common.security.annotation.CurrentUser;
import com.pawtrail.common.security.principal.CustomUserPrincipal;
import com.pawtrail.notification.application.dto.output.NotificationCardOutput;
import com.pawtrail.notification.application.dto.output.NotificationSettingOutput;
import com.pawtrail.notification.application.dto.output.UnreadCountOutput;
import com.pawtrail.notification.application.service.NotificationService;
import com.pawtrail.notification.application.service.NotificationSettingService;
import com.pawtrail.notification.presentation.request.NotificationSettingUpdateRequest;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.PositiveOrZero;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * 알림을 보고 읽고, 수신 설정을 바꾸는 공개 API 여섯입니다.
 *
 * 게이트웨이의 /api/v1/notifications/** 라우트로 들어옵니다. 전부 로그인이 필요하고 자기 것만 다룹니다.
 *
 *   GET   /api/v1/notifications                 목록 — 최신순 · 쪽
 *   PATCH /api/v1/notifications/{id}/read       읽음
 *   PATCH /api/v1/notifications/read-all        모두 읽음
 *   GET   /api/v1/notifications/unread-count    안 읽은 수 — 헤더 벨이 폴링
 *   GET   /api/v1/notifications/settings        수신 설정
 *   PATCH /api/v1/notifications/settings        수신 설정 바꾸기
 *
 * 읽음 · 모두 읽음은 본문 없이 200 을 돌려줍니다. user 의 즐겨찾기 담기 · 해제와 같습니다.
 * 벨의 숫자는 화면이 줄이고 다음 폴링이 맞춥니다.
 */
@RestController
@RequestMapping("/api/v1/notifications")
@RequiredArgsConstructor
public class NotificationController {

    private final NotificationService notificationService;
    private final NotificationSettingService notificationSettingService;

    /**
     * 쪽 번호와 크기만 받고 정렬은 받지 않습니다. 최신순으로 고정입니다.
     * 정렬을 받으면 요청마다 차례가 흔들릴 수 있습니다. report 의 목록과 같은 규칙입니다.
     */
    @GetMapping
    public ResponseEntity<CommonApiResponse<PageResponse<NotificationCardOutput>>> getMine(
            @CurrentUser CustomUserPrincipal principal,
            @RequestParam(defaultValue = "0") @PositiveOrZero(message = "page 는 0 이상이어야 합니다") int page,
            @RequestParam(defaultValue = "20")
            @Min(value = 1, message = "size 는 1 이상이어야 합니다")
            @Max(value = 100, message = "size 는 100 이하여야 합니다") int size) {
        PageResponse<NotificationCardOutput> response = notificationService.getMine(principal.accountId(), page, size);
        return ResponseEntity.ok(CommonApiResponse.success(response));
    }

    @PatchMapping("/{notificationId}/read")
    public ResponseEntity<CommonApiResponse<Void>> read(
            @CurrentUser CustomUserPrincipal principal,
            @PathVariable UUID notificationId) {
        notificationService.read(principal.accountId(), notificationId);
        return ResponseEntity.ok(CommonApiResponse.success(null));
    }

    @PatchMapping("/read-all")
    public ResponseEntity<CommonApiResponse<Void>> readAll(@CurrentUser CustomUserPrincipal principal) {
        notificationService.readAll(principal.accountId());
        return ResponseEntity.ok(CommonApiResponse.success(null));
    }

    @GetMapping("/unread-count")
    public ResponseEntity<CommonApiResponse<UnreadCountOutput>> countUnread(
            @CurrentUser CustomUserPrincipal principal) {
        return ResponseEntity.ok(CommonApiResponse.success(notificationService.countUnread(principal.accountId())));
    }

    @GetMapping("/settings")
    public ResponseEntity<CommonApiResponse<NotificationSettingOutput>> getSettings(
            @CurrentUser CustomUserPrincipal principal) {
        return ResponseEntity.ok(CommonApiResponse.success(notificationSettingService.get(principal.accountId())));
    }

    @PatchMapping("/settings")
    public ResponseEntity<CommonApiResponse<NotificationSettingOutput>> updateSettings(
            @CurrentUser CustomUserPrincipal principal,
            @RequestBody NotificationSettingUpdateRequest request) {
        NotificationSettingOutput response =
                notificationSettingService.update(principal.accountId(), request.toInput());
        return ResponseEntity.ok(CommonApiResponse.success(response));
    }
}
