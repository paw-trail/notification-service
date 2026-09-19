package com.pawtrail.notification.domain.exception;

import com.pawtrail.common.exception.ErrorCode;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;

/**
 * notification 서비스가 내는 오류입니다.
 *
 * 상수 이름이 곧 응답의 code 이자 화면과의 약속입니다.
 * 이름을 바꾸면 화면이 그 오류를 못 알아보므로 바꾸지 않습니다.
 *
 * 입력 형식이 틀린 것은 여기 두지 않고 공통의 VALIDATION_FAILED 를 씁니다.
 */
@Getter
@RequiredArgsConstructor
public enum NotificationErrorCode implements ErrorCode {

    // 그런 알림이 없음 — 읽음 처리에서 남
    //
    // * 남의 알림도 같은 코드로 답함 — 있는지조차 알리지 않음
    // * 같은 장소의 안 읽은 조건 알림은 새 알림이 오면 지우고 갈아 끼우므로
    //   화면이 들고 있던 알림이 사라졌을 수 있음 — 화면은 목록을 새로 부르면 됨
    // * 공통의 RESOURCE_NOT_FOUND 를 쓰지 않는 이유
    //   그 코드는 없는 주소를 불렀을 때도 나가 둘이 섞임
    NOTIFICATION_NOT_FOUND(HttpStatus.NOT_FOUND, "알림을 찾을 수 없습니다.");

    private final HttpStatus httpStatus;
    private final String message;

    @Override
    public String getCode() {
        return name();
    }
}
