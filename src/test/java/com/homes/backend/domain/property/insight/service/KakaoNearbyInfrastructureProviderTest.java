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

class KakaoNearbyInfrastructureProviderTest {

    @Test
    void returnsCountsAndNearestPlacesWithinOneKilometer() {
        RestTemplate restTemplate = new RestTemplate();
        MockRestServiceServer server = MockRestServiceServer.bindTo(restTemplate).build();
        expect(server, "HP8", 8, "한일병원", 240);
        expect(server, "MT1", 2, "하나로마트", 520);
        expect(server, "CT1", 3, "도봉문화원", 780);
        KakaoNearbyInfrastructureProvider provider =
                new KakaoNearbyInfrastructureProvider(restTemplate, new ObjectMapper(), "test-key");

        var result = provider.evaluate(new GeometryFactory()
                .createPoint(new Coordinate(127.0471, 37.6688))).orElseThrow();

        assertThat(result.description()).contains("병원 8곳", "대형마트 2곳", "문화시설 3곳");
        assertThat(result.evidence()).extracting(evidence -> evidence.value())
                .containsExactly(
                        "8곳 · 가까운 곳 한일병원 240m",
                        "2곳 · 가까운 곳 하나로마트 520m",
                        "3곳 · 가까운 곳 도봉문화원 780m");
        server.verify();
    }

    private void expect(MockRestServiceServer server, String code, int count, String name, int distance) {
        String body = """
                {"meta":{"total_count":%d},"documents":[{"place_name":"%s","distance":"%d"}]}
                """.formatted(count, name, distance);
        server.expect(requestTo(containsString("category_group_code=" + code)))
                .andRespond(withSuccess(body, MediaType.APPLICATION_JSON));
    }
}
