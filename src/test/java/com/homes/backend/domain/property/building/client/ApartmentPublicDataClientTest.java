package com.homes.backend.domain.property.building.client;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.homes.backend.domain.property.building.config.PublicDataApiProperties;
import com.homes.backend.global.geocoding.ResolvedAddress;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.http.HttpMethod.GET;
import static org.springframework.test.web.client.ExpectedCount.once;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

class ApartmentPublicDataClientTest {
    private final ObjectMapper objectMapper = new ObjectMapper();

    /**
     * Verifies that a versioned list response matches a complex by building name.
     */
    @Test
    void readsVersionedApartmentListArrayAndMatchesByBuildingName() {
        PublicDataApiProperties properties = properties();
        ApartmentComplexClient client = new ApartmentComplexClient(properties, objectMapper);
        MockRestServiceServer server = MockRestServiceServer.bindTo(client.restTemplate).build();
        ResolvedAddress address = new ResolvedAddress("서울특별시 도봉구 방학동 717-2", 37.66, 127.04,
                "1132010600", "11320", "10600", "0", "0717", "0002", "도봉롯데캐슬골든파크");

        server.expect(once(), requestTo("https://example.test/AptListService4/getLegaldongAptList4?serviceKey=test-key&bjdCode=1132010600&numOfRows=100&pageNo=1"))
                .andExpect(method(GET))
                .andRespond(withSuccess("""
                        {"response":{"header":{"resultCode":"00"},"body":{"items":[
                          {"kaptCode":"A10019968","kaptName":"우암센스뷰","as1":"서울특별시","as2":"도봉구","as3":"방학동","as4":""},
                          {"kaptCode":"A10020507","kaptName":"도봉롯데캐슬골든파크","as1":"서울특별시","as2":"도봉구","as3":"방학동","as4":""}
                        ]}}}
                        """, MediaType.APPLICATION_JSON));

        ApartmentComplex result = client.findByAddress(address).orElseThrow();

        assertThat(result.kaptCode()).isEqualTo("A10020507");
        assertThat(result.address()).isEqualTo("서울특별시 도봉구 방학동");
        server.verify();
    }

    /**
     * Verifies that an unmatched complex is not selected from an address-only result.
     */
    @Test
    void doesNotSelectArbitraryComplexWithoutBuildingNameMatch() {
        PublicDataApiProperties properties = properties();
        ApartmentComplexClient client = new ApartmentComplexClient(properties, objectMapper);
        MockRestServiceServer server = MockRestServiceServer.bindTo(client.restTemplate).build();
        ResolvedAddress address = new ResolvedAddress("서울특별시 도봉구 방학동 123-4", 37.66, 127.04,
                "1132010600", "11320", "10600", "0", "0123", "0004", null);

        server.expect(once(), requestTo("https://example.test/AptListService4/getLegaldongAptList4?serviceKey=test-key&bjdCode=1132010600&numOfRows=100&pageNo=1"))
                .andRespond(withSuccess("""
                        {"response":{"header":{"resultCode":"00"},"body":{"items":[
                          {"kaptCode":"A10019968","kaptName":"우암센스뷰","as1":"서울특별시","as2":"도봉구","as3":"방학동"}
                        ]}}}
                        """, MediaType.APPLICATION_JSON));

        assertThat(client.findByAddress(address)).isEmpty();
        server.verify();
    }

    /**
     * Verifies that a singleton basic-information response body is parsed correctly.
     */
    @Test
    void readsApartmentBasisBodyItem() {
        PublicDataApiProperties properties = properties();
        ApartmentBasisClient client = new ApartmentBasisClient(properties, objectMapper);
        MockRestServiceServer server = MockRestServiceServer.bindTo(client.restTemplate).build();

        server.expect(once(), requestTo("https://example.test/AptBasisInfoServiceV5/getAphusBassInfoV5?serviceKey=test-key&kaptCode=A10020507"))
                .andRespond(withSuccess("""
                        {"response":{"header":{"resultCode":"00"},"body":{"item":{
                          "kaptCode":"A10020507","kaptName":"도봉롯데캐슬골든파크",
                          "kaptUsedate":"2025-02-28","kaptdaCnt":"282","kaptDongCnt":"2",
                          "kaptTopFloor":"23","codeHallNm":"계단식","codeHeatNm":"개별난방"
                        }}}}
                        """, MediaType.APPLICATION_JSON));

        ApartmentBasicInformation result = client.findBasicInformation("A10020507").orElseThrow();

        assertThat(result.householdCount()).isEqualTo(282);
        assertThat(result.buildingCount()).isEqualTo(2);
        assertThat(result.highestFloor()).isEqualTo(23);
        assertThat(result.approvalDate()).hasToString("2025-02-28");
        server.verify();
    }

    /**
     * Creates the isolated public-data client configuration used by these tests.
     */
    private PublicDataApiProperties properties() {
        PublicDataApiProperties properties = new PublicDataApiProperties();
        properties.setServiceKey("test-key");
        properties.setApartmentListUrl("https://example.test/AptListService4");
        properties.setApartmentBasisUrl("https://example.test/AptBasisInfoServiceV5");
        return properties;
    }
}
