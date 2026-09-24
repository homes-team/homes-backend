package com.homes.backend.domain.property.insight.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.GeometryFactory;
import org.locationtech.jts.geom.Point;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestTemplate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.springframework.test.web.client.ExpectedCount.once;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.header;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

class KakaoNearbySchoolProviderTest {

    @Test
    void returnsNearestSchoolNameAndDistanceForEachSchoolLevel() {
        RestTemplate restTemplate = new RestTemplate();
        MockRestServiceServer server = MockRestServiceServer.bindTo(restTemplate).build();
        expectSchoolResponse(server, "도봉초등학교", "교육,학문 > 학교 > 초등학교", "300");
        expectSchoolResponse(server, "도봉중학교", "교육,학문 > 학교 > 중학교", "900");
        expectSchoolResponse(server, "도봉고등학교", "교육,학문 > 학교 > 고등학교", "1500");
        KakaoNearbySchoolProvider provider =
                new KakaoNearbySchoolProvider(restTemplate, new ObjectMapper(), "test-key");
        Point coordinate = new GeometryFactory().createPoint(new Coordinate(127.0471, 37.6688));

        KakaoNearbySchoolProvider.Result result = provider.evaluate(coordinate).orElseThrow();

        assertThat(result.score()).isEqualTo(80.0);
        assertThat(result.description()).contains("도봉초등학교", "도봉중학교", "도봉고등학교");
        assertThat(result.evidence()).extracting(evidence -> evidence.value())
                .containsExactly("도봉초등학교 · 300m", "도봉중학교 · 900m", "도봉고등학교 · 1500m");
        assertThat(result.evidence()).extracting(evidence -> evidence.label())
                .containsExactly("가장 가까운 초등학교", "가장 가까운 중학교", "가장 가까운 고등학교");
        server.verify();
    }

    private void expectSchoolResponse(
            MockRestServiceServer server,
            String name,
            String category,
            String distance
    ) {
        String body = """
                {"documents":[{
                  "place_name":"%s",
                  "category_name":"%s",
                  "distance":"%s"
                }]}
                """.formatted(name, category, distance);
        server.expect(once(), requestTo(containsString("category_group_code=SC4")))
                .andExpect(method(HttpMethod.GET))
                .andExpect(header("Authorization", "KakaoAK test-key"))
                .andRespond(withSuccess(body, MediaType.APPLICATION_JSON));
    }
}
