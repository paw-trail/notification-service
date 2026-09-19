package com.pawtrail.notification.domain.provider.dto;

import java.util.List;
import java.util.UUID;

/**
 * 즐겨찾기 명단 한 쪽입니다.
 *
 * @param accountIds 그 쪽에 담긴 계정 식별자입니다.
 * @param last       마지막 쪽인지입니다. true 면 더 부르지 않습니다.
 */
public record FavoritePage(List<UUID> accountIds, boolean last) {
}
