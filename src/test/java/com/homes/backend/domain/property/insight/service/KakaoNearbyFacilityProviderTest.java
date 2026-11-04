package com.homes.backend.domain.property.insight.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.homes.backend.domain.property.insight.dto.NearbyFacilitiesRespDto.FacilityType;
import org.junit.jupiter.api.Test;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.GeometryFactory;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestTemplate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

class KakaoNearbyFacilityProviderTest {

    @Test
    void returnsCoordinateBearingFacilities() {
        RestTemplate restTemplate = new RestTemplate();
        MockRestServiceServer server = MockRestServiceServer.bindTo(restTemplate).build();
        server.expect(requestTo(containsString("category_group_code=HP8")))
                .andRespond(withSuccess("""
                        {"documents":[{
                          "id":"hospital-1","place_name":"한일병원","distance":"240",
                          "x":"127.0475","y":"37.6690","road_address_name":"서울 도봉구 테스트로 1"
                        }]}
                        """, MediaType.APPLICATION_JSON));
        KakaoNearbyFacilityProvider provider =
                new KakaoNearbyFacilityProvider(restTemplate, new ObjectMapper(), "test-key");

        var result = provider.findNearby(
                new GeometryFactory().createPoint(new Coordinate(127.0471, 37.6688)),
                FacilityType.HOSPITAL,
                2_000,
                5
        );

        assertThat(result).singleElement().satisfies(place -> {
            assertThat(place.id()).isEqualTo("hospital-1");
            assertThat(place.name()).isEqualTo("한일병원");
            assertThat(place.latitude()).isEqualTo(37.6690);
            assertThat(place.longitude()).isEqualTo(127.0475);
            assertThat(place.distanceMeters()).isEqualTo(240);
            assertThat(place.address()).isEqualTo("서울 도봉구 테스트로 1");
        });
        server.verify();
    }

    @Test
    void returnsEmptyListWhenOneKakaoQueryFails() {
        RestTemplate restTemplate = new RestTemplate();
        MockRestServiceServer server = MockRestServiceServer.bindTo(restTemplate).build();
        server.expect(requestTo(containsString("query=%EA%B3%B5%EC%9B%90")))
                .andRespond(withSuccess("not-json", MediaType.TEXT_PLAIN));
        KakaoNearbyFacilityProvider provider =
                new KakaoNearbyFacilityProvider(restTemplate, new ObjectMapper(), "test-key");

        var result = provider.findNearby(
                new GeometryFactory().createPoint(new Coordinate(127.0471, 37.6688)),
                FacilityType.PARK,
                2_000,
                5
        );

        assertThat(result).isEmpty();
        server.verify();
    }

    @Test
    void excludesKeywordMatchesThatAreNotCategorizedAsParks() {
        RestTemplate restTemplate = new RestTemplate();
        MockRestServiceServer server = MockRestServiceServer.bindTo(restTemplate).build();
        server.expect(requestTo(containsString("query=%EA%B3%B5%EC%9B%90")))
                .andRespond(withSuccess("""
                        {"documents":[
                          {"id":"apartment-1","place_name":"공원아파트","category_name":"부동산 > 주거시설 > 아파트","distance":"20","x":"127.0472","y":"37.6689"},
                          {"id":"park-1","place_name":"초안산근린공원","category_name":"여행 > 관광,명소 > 공원","distance":"450","x":"127.0480","y":"37.6700"}
                        ]}
                        """, MediaType.APPLICATION_JSON));
        KakaoNearbyFacilityProvider provider =
                new KakaoNearbyFacilityProvider(restTemplate, new ObjectMapper(), "test-key");

        var result = provider.findNearby(
                new GeometryFactory().createPoint(new Coordinate(127.0471, 37.6688)),
                FacilityType.PARK,
                2_000,
                5
        );

        assertThat(result).extracting(KakaoNearbyFacilityProvider.Place::name)
                .containsExactly("초안산근린공원");
        server.verify();
    }

    @Test
    void continuesParkSearchUntilEnoughFilteredResultsAreFound() {
        RestTemplate restTemplate = new RestTemplate();
        MockRestServiceServer server = MockRestServiceServer.bindTo(restTemplate).build();
        server.expect(requestTo(containsString("page=1")))
                .andRespond(withSuccess("""
                        {"meta":{"is_end":false},"documents":[
                          {"id":"apartment-1","place_name":"공원아파트","category_name":"부동산 > 주거시설 > 아파트","distance":"20","x":"127.0472","y":"37.6689"}
                        ]}
                        """, MediaType.APPLICATION_JSON));
        server.expect(requestTo(containsString("page=2")))
                .andRespond(withSuccess("""
                        {"meta":{"is_end":true},"documents":[
                          {"id":"park-1","place_name":"초안산근린공원","category_name":"여행 > 관광,명소 > 공원","distance":"450","x":"127.0480","y":"37.6700"}
                        ]}
                        """, MediaType.APPLICATION_JSON));
        KakaoNearbyFacilityProvider provider =
                new KakaoNearbyFacilityProvider(restTemplate, new ObjectMapper(), "test-key");

        var result = provider.findNearby(
                new GeometryFactory().createPoint(new Coordinate(127.0471, 37.6688)),
                FacilityType.PARK,
                2_000,
                1
        );

        assertThat(result).extracting(KakaoNearbyFacilityProvider.Place::name)
                .containsExactly("초안산근린공원");
        server.verify();
    }
}
