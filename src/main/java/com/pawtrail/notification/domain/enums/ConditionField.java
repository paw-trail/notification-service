package com.pawtrail.notification.domain.enums;

import java.util.Arrays;
import java.util.Optional;

/**
 * 조건 20칸의 이름과 화면에 보이는 이름표입니다. 조건 변경 알림의 본문을 지을 때 씁니다.
 *
 * <b>이름표는 policy 의 FieldSpec 과 같은 말입니다.</b>
 * 사용자가 알림을 누르면 장소 상세에서 policy 의 조건 칸 이름을 보게 되므로 같은 칸은 같은 말이어야 합니다.
 * 두 레포가 달라 자동으로 맞춰 보지 못하므로, policy 의 이름표가 바뀌면 여기도 함께 고칩니다.
 * verdict 의 ConditionField 와 글자까지 같습니다.
 *
 * 칸이 늘었는데 여기를 고치지 않으면 그 칸은 이름 대신 "외 N개" 의 수에만 들어갑니다.
 * 알림이 깨지지는 않습니다.
 *
 * 선언 순서가 칸 순서입니다.
 */
public enum ConditionField {

    SCOPE("scope", "동반 범위"),
    GUIDE_DOG_ONLY("guideDogOnly", "안내견 한정"),
    PET_ONLY("petOnly", "반려견 동반 전용"),
    INDOOR_ALLOWED("indoorAllowed", "실내 동반"),
    OUTDOOR_ALLOWED("outdoorAllowed", "실외 동반"),
    MAX_WEIGHT_KG("maxWeightKg", "체중 제한"),
    WEIGHT_INCLUSIVE("weightInclusive", "체중 기준"),
    MAX_COUNT("maxCount", "마릿수 제한"),
    SIZE_RULE("sizeRule", "크기 제한"),
    BREED_RULE("breedRule", "견종 제한"),
    CARRIER_REQUIRED("carrierRequired", "이동장"),
    LEASH_REQUIRED("leashRequired", "목줄"),
    EXCLUDED_ZONES("excludedZones", "동반 불가 구역"),
    ALLOWED_ZONES_ONLY("allowedZonesOnly", "동반 가능 구역"),
    EXCLUDED_DAYS("excludedDays", "동반 불가일"),
    EXTRA_FEE_AMOUNT("extraFeeAmount", "추가 요금"),
    EXTRA_FEE_UNIT("extraFeeUnit", "요금 기준"),
    REQUIRED_ITEMS("requiredItems", "준비물"),
    VACCINE_PROOF("vaccineProof", "접종 증명"),
    ADVANCE_INQUIRY("advanceInquiry", "사전 문의");

    // policy 가 policy.changed 의 changedFields 에 싣는 칸 이름 — camelCase 이며 DB 컬럼 이름이 아님
    private final String fieldName;

    private final String label;

    ConditionField(String fieldName, String label) {
        this.fieldName = fieldName;
        this.label = label;
    }

    public String fieldName() {
        return fieldName;
    }

    public String label() {
        return label;
    }

    /**
     * 칸 이름으로 찾습니다. 모르는 이름이면 비어 있습니다.
     */
    public static Optional<ConditionField> ofName(String fieldName) {
        return Arrays.stream(values())
                .filter(field -> field.fieldName.equals(fieldName))
                .findFirst();
    }
}
