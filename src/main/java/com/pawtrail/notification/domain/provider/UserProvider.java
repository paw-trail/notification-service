package com.pawtrail.notification.domain.provider;

import com.pawtrail.notification.domain.provider.dto.FavoritePage;
import java.util.UUID;

/**
 * user 서비스에서 즐겨찾기 명단을 받아오는 약속입니다.
 */
public interface UserProvider {

    /**
     * 그 장소를 즐겨찾기한 사람들의 계정 식별자를 한 쪽 받아옵니다. 조건 변경 알림을 받을 사람을 모을 때 씁니다.
     *
     * 쪽 번호는 0 부터이고, 마지막 쪽이면 last 가 true 입니다.
     * user 가 즐겨찾기 식별자 순으로 돌려주므로 쪽을 넘기는 사이 같은 사람이 두 번 나오거나 빠지지 않습니다.
     *
     * <b>못 부르면 예외를 던집니다.</b> place 처럼 null 로 삼키지 않습니다.
     * 명단을 모르면 누구에게 알릴지 모르는 것이라 이 이벤트를 처음부터 다시 처리해야 합니다.
     * 예외가 소비자까지 올라가면 공통 오류 처리기가 세 번 다시 시도하고, 끝내 안 되면 .dlq 로 보냅니다.
     */
    FavoritePage findFavoriteAccountIds(UUID placeId, int page);
}
