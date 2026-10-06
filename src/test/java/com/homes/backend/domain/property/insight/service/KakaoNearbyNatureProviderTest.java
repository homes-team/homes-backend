package com.homes.backend.domain.property.insight.service;

import com.fasterxml.jackson.databind.ObjectMapper;
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

class KakaoNearbyNatureProviderTest {

    @Test
    void calculatesNatureScoreFromNearestParkAndParkDensity() {
        RestTemplate restTemplate = new RestTemplate();
        MockRestServiceServer server = MockRestServiceServer.bindTo(restTemplate).build();
        server.expect(requestTo(containsString("query=%EA%B3%B5%EC%9B%90")))
                .andExpect(requestTo(containsString("radius=2000")))
                .andRespond(withSuccess("""
                        {"meta":{"total_count":7},"documents":[
                          {"place_name":"테스트근린공원","distance":"350"}
                        ]}
                        """, MediaType.APPLICATION_JSON));
        KakaoNearbyNatureProvider provider =
                new KakaoNearbyNatureProvider(restTemplate, new ObjectMapper(), "test-key");

        var result = provider.evaluate(new GeometryFactory()
                .createPoint(new Coordinate(127.0471, 37.6688))).orElseThrow();

        assertThat(result.score()).isEqualTo(80.5);
        assertThat(result.description()).contains("테스트근린공원", "350m", "7곳");
        assertThat(result.evidence()).extracting(evidence -> evidence.code())
                .containsExactly("NEAREST_PARK_DISTANCE", "PARK_COUNT_WITHIN_2KM");
        assertThat(result.calculation().policyVersion()).isEqualTo("NATIONWIDE_NATURE_V1");
        server.verify();
    }

    @Test
    void returnsLowAvailableScoreWhenNoParkIsFound() {
        RestTemplate restTemplate = new RestTemplate();
        MockRestServiceServer server = MockRestServiceServer.bindTo(restTemplate).build();
        server.expect(requestTo(containsString("query=%EA%B3%B5%EC%9B%90")))
                .andRespond(withSuccess("""
                        {"meta":{"total_count":0},"documents":[]}
                        """, MediaType.APPLICATION_JSON));
        KakaoNearbyNatureProvider provider =
                new KakaoNearbyNatureProvider(restTemplate, new ObjectMapper(), "test-key");

        var result = provider.evaluate(new GeometryFactory()
                .createPoint(new Coordinate(127.0471, 37.6688))).orElseThrow();

        assertThat(result.score()).isEqualTo(14.0);
        assertThat(result.description()).contains("확인된 공원이 없습니다");
        server.verify();
    }
}
