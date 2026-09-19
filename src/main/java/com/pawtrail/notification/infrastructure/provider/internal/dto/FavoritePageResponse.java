package com.pawtrail.notification.infrastructure.provider.internal.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import java.util.List;

/**
 * GET /internal/favorites?placeId= 의 data 입니다. user 가 공통 PageResponse 로 보냅니다.
 *
 * 봉투(CommonApiResponse)를 벗긴 안쪽만 담습니다. 벗기는 일은 UserProviderImpl 이 합니다.
 * 공통 PageResponse 를 그대로 쓰지 않고 따로 둡니다. 받는 모양을 이쪽이 정해 두면
 * user 가 칸을 더해도 깨지지 않습니다. 모르는 칸은 무시합니다.
 *
 * 계정 식별자를 문자열로 받습니다. UUID 로 바꾸는 일은 구현이 합니다.
 *
 * @param content 그 쪽의 계정 식별자입니다.
 * @param page    쪽 정보입니다. 마지막 쪽인지를 여기서 가립니다.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public record FavoritePageResponse(List<String> content, PageInfo page) {

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record PageInfo(int number, int size, long totalElements, int totalPages) {
    }
}
