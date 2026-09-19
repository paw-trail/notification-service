package com.pawtrail.notification.infrastructure.provider.internal;

import com.pawtrail.common.response.CommonApiResponse;
import com.pawtrail.notification.domain.provider.UserProvider;
import com.pawtrail.notification.domain.provider.dto.FavoritePage;
import com.pawtrail.notification.infrastructure.provider.internal.dto.FavoritePageResponse;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

/**
 * 도메인이 선언한 약속을 user 서비스 호출로 구현합니다.
 *
 * place 를 부르는 PlaceProviderImpl 과 달리 실패를 삼키지 않고 예외로 올립니다.
 * 이름은 비워도 목록이 서지만, 명단은 비우면 알림을 받을 사람이 사라지기 때문입니다.
 */
@Slf4j
@Component
public class UserProviderImpl implements UserProvider {

    private static final String BASE_URL = "lb://user-service";

    /**
     * 한 쪽의 크기입니다. user 의 기본값과 같습니다.
     *
     * 인기 장소면 즐겨찾기가 수천 명일 수 있는데, 100명씩이면 수십 번에 끝납니다.
     * user 가 이 조회를 만들 때 20 이면 수백 번 돈다고 보고 100 으로 정했습니다.
     */
    static final int PAGE_SIZE = 100;

    private final RestClient restClient;

    /**
     * 빌더를 주입받아 RestClient 를 만듭니다.
     *
     * @Qualifier 를 반드시 붙여야 합니다. 이유는 PlaceProviderImpl 에 적어 두었습니다.
     */
    public UserProviderImpl(
            @Qualifier("internalRestClientBuilder") RestClient.Builder builder) {

        this.restClient = builder.baseUrl(BASE_URL).build();
    }

    /**
     * 한 쪽을 받아옵니다.
     *
     * 잡는 범위를 Exception 으로 두고 무엇이든 IllegalStateException 으로 감싸 다시 던집니다.
     * 연결 거부 · 시간 초과 · 유레카가 서비스를 못 찾는 것까지 결과가 모두 같습니다.
     * 명단을 못 받았다는 것 하나이고, 할 일도 이벤트를 다시 처리하는 것 하나입니다.
     * 어느 장소 몇 쪽에서 멈췄는지를 메시지에 남겨 .dlq 를 볼 때 찾기 쉽게 합니다.
     */
    @Override
    public FavoritePage findFavoriteAccountIds(UUID placeId, int page) {
        CommonApiResponse<FavoritePageResponse> response;
        try {
            response = restClient.get()
                    .uri(builder -> builder.path("/internal/favorites")
                            .queryParam("placeId", placeId)
                            .queryParam("page", page)
                            .queryParam("size", PAGE_SIZE)
                            .build())
                    .retrieve()
                    .body(new ParameterizedTypeReference<>() {});
        } catch (Exception e) {
            throw new IllegalStateException(
                    "즐겨찾기 명단을 받지 못했습니다: placeId=" + placeId + ", page=" + page, e);
        }

        if (response == null || response.getData() == null || response.getData().content() == null) {
            throw new IllegalStateException(
                    "즐겨찾기 명단 응답이 비어 있습니다: placeId=" + placeId + ", page=" + page);
        }

        FavoritePageResponse data = response.getData();
        return new FavoritePage(toUuids(data.content()), isLast(data, page));
    }

    /**
     * 마지막 쪽인지 가립니다.
     *
     * 쪽 정보가 있으면 그것으로 보고, 없으면 쪽이 덜 찼는지로 봅니다.
     * 전체가 0명이면 쪽 수도 0 이라 첫 쪽이 곧 마지막 쪽입니다.
     */
    private static boolean isLast(FavoritePageResponse data, int page) {
        if (data.page() == null) {
            return data.content().size() < PAGE_SIZE;
        }
        return page + 1 >= data.page().totalPages();
    }

    /**
     * 식별자를 UUID 로 바꿉니다. 형식이 어긋난 원소는 건너뜁니다.
     *
     * user 의 DB 에서 나온 값이라 어긋날 일은 없으나, 하나 때문에 나머지 사람의 알림까지 막을 이유는 없습니다.
     */
    private static List<UUID> toUuids(List<String> values) {
        List<UUID> result = new ArrayList<>(values.size());
        for (String value : values) {
            try {
                result.add(UUID.fromString(value));
            } catch (IllegalArgumentException | NullPointerException e) {
                log.warn("계정 식별자가 UUID 형식이 아닙니다: {}", value);
            }
        }
        return result;
    }
}
