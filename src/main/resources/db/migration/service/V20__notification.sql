-- 이 서비스의 첫 마이그레이션 스크립트입니다.
-- V1 부터 V19 는 공통 모듈이 사용하는 대역이므로 쓰지 않습니다.
--
-- 이미 적용된 스크립트는 수정하지 않습니다.
-- 내용이 바뀌면 체크섬이 달라져 다음 기동이 실패합니다.
-- 변경이 필요하면 다음 번호로 새 스크립트를 만듭니다.
--
-- notif_db 에는 이 표 둘과 공통 대역의 outbox · processed_event 가 있습니다.
-- notification 은 processed_event 만 씁니다.
--   processed_event  받은 이벤트(policy.changed · report.resolved · account.withdrawn)를 한 번만 처리하는 장치
--   outbox           공통 대역이라 생기지만 쓰지 않습니다. 이 서비스는 이벤트를 발행하지 않습니다.
--
-- 계정 · 장소는 다른 서비스의 것이라 외래키를 걸지 않습니다.
-- 서비스가 갈려 DB 가 다르기 때문입니다.

-- =============================================================================
-- notification
-- =============================================================================
-- 한 사람에게 보낸 알림 한 건입니다.
--
-- 두 종류가 있습니다.
--   POLICY_CHANGED   즐겨찾기한 장소의 동반 조건이 바뀜 (policy.changed)
--   REPORT_RESOLVED  올린 제보를 관리자가 처리함 (report.resolved)
--
-- 알림은 만드는 순간이 곧 보내는 순간이라 보낸 시각을 따로 두지 않습니다. created_at 이 그 값입니다.

CREATE TABLE notification
(
    -- PK 는 모든 테이블이 uuid 입니다.
    -- 애플리케이션이 Hibernate 의 @UuidGenerator(style = VERSION_7) 로 생성해 넣으므로
    -- 여기에 기본값을 지정하지 않습니다.
    id          uuid          PRIMARY KEY,

    -- 알림을 받는 계정입니다. auth_db 의 account.id 를 가리키나 외래키를 걸지 않습니다.
    -- 탈퇴하면 account.withdrawn 을 받아 이 계정의 행을 전부 지웁니다.
    account_id  uuid          NOT NULL,

    -- 알림 종류입니다. POLICY_CHANGED · REPORT_RESOLVED
    --
    -- CHECK 로 값 목록을 막지 않습니다.
    -- 종류가 늘면 마이그레이션이 하나 더 필요해지고, 값은 애플리케이션의 열거형이 막습니다.
    notif_type  varchar(24)   NOT NULL,

    -- 알림이 가리키는 장소입니다. place_db 의 place.id 를 가리킵니다.
    --
    -- 두 종류 모두 채웁니다.
    -- 조건 변경은 그 장소이고, 제보 결과는 제보한 장소(후기 신고는 그 후기가 달린 장소)입니다.
    -- 알림을 누르면 이 장소의 상세로 가고, 목록을 열 때 이 값으로 장소 이름을 받아 옵니다.
    place_id    uuid          NOT NULL,

    -- 알림 제목입니다. 장소 이름을 넣지 않습니다.
    -- 장소 이름은 place 가 바꿀 수 있는 값이라 목록을 열 때 받아 채웁니다.
    title       varchar(100)  NOT NULL,

    -- 알림 본문입니다.
    -- 조건 변경은 바뀐 조건 칸의 이름을, 제보 결과는 관리자 메모를 그대로 담습니다.
    -- 관리자 메모(report.memo)가 500자까지라 같은 폭으로 둡니다.
    body        varchar(500)  NOT NULL,

    -- 읽은 시각입니다. 비어 있으면 안 읽은 알림이고, 헤더 벨의 숫자가 이것을 셉니다.
    -- 이미 읽은 알림을 다시 읽어도 처음 읽은 시각을 그대로 둡니다.
    read_at     timestamp,

    created_at  timestamp     NOT NULL,
    created_by  varchar(45)   NOT NULL,
    updated_at  timestamp     NOT NULL,
    updated_by  varchar(45)   NOT NULL,

    -- 공통 규약이라 두지만 쓰지 않습니다.
    -- 탈퇴한 계정의 알림과 새 알림으로 갈아 끼운 알림은 행째 지웁니다.
    deleted_at  timestamp,
    deleted_by  varchar(45)
);

-- 알림 목록 (GET /api/v1/notifications) 이 이 순서로 읽습니다.
-- 안 읽은 수를 셀 때도 계정으로 좁히는 데 이 인덱스를 탑니다.
-- 한 사람의 알림이라 수가 적어 안 읽은 알림만 담는 인덱스를 따로 두지 않습니다.
CREATE INDEX idx_notification_account_created
    ON notification (account_id, created_at DESC);

-- =============================================================================
-- notification_setting
-- =============================================================================
-- 알림 종류마다 받을지를 사람마다 담습니다.
--
-- 행이 없으면 전부 받는 것으로 봅니다. 미리 만들지 않습니다.
-- 사용자가 설정을 처음 바꾸는 순간 행이 생깁니다.
-- 그래서 가입 이벤트(account.created)를 받을 필요가 없습니다.

CREATE TABLE notification_setting
(
    -- 설정의 주인입니다. 한 사람에 한 행이라 계정 식별자를 그대로 PK 로 씁니다.
    account_id       uuid         PRIMARY KEY,

    -- 조건 변경 알림을 받을지입니다.
    policy_changed   boolean      NOT NULL DEFAULT true,

    -- 제보 처리 결과 알림을 받을지입니다.
    report_resolved  boolean      NOT NULL DEFAULT true,

    created_at       timestamp    NOT NULL,
    created_by       varchar(45)  NOT NULL,
    updated_at       timestamp    NOT NULL,
    updated_by       varchar(45)  NOT NULL,

    -- 탈퇴 표시로 씁니다.
    -- 탈퇴하면 이 행을 지우지 않고 deleted_at 을 찍어 남깁니다. 행이 없던 사람은 표시 행을 새로 만듭니다.
    -- 탈퇴보다 늦게 도착한 알림거리가 이 표시를 보고 건너뜁니다.
    deleted_at       timestamp,
    deleted_by       varchar(45)
);

COMMENT ON TABLE notification IS '사용자에게 보낸 알림 — 조건 변경 · 제보 처리 결과';
COMMENT ON COLUMN notification.place_id IS '알림이 가리키는 장소 — 누르면 이 장소의 상세로 감';
COMMENT ON COLUMN notification.read_at IS '읽은 시각 — 비어 있으면 안 읽음';
COMMENT ON TABLE notification_setting IS '알림 종류별 수신 설정 — 행이 없으면 전부 받음 · deleted_at 은 탈퇴 표시';
