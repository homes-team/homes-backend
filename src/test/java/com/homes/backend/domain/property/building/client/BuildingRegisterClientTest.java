package com.homes.backend.domain.property.building.client;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.homes.backend.domain.property.building.config.PublicDataApiProperties;
import com.homes.backend.global.geocoding.ResolvedAddress;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.web.client.MockRestServiceServer;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.http.HttpMethod.GET;
import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.test.web.client.ExpectedCount.once;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.queryParam;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

class BuildingRegisterClientTest {
    private BuildingRegisterClient client;
    private MockRestServiceServer server;
    private ResolvedAddress address;

    @BeforeEach
    void setUp() {
        PublicDataApiProperties properties = new PublicDataApiProperties();
        properties.setServiceKey("encoded%2Fkey%3D%3D");
        properties.setBuildingRegisterUrl("https://example.test/BldRgstHubService");
        client = new BuildingRegisterClient(properties, new ObjectMapper());
        server = MockRestServiceServer.bindTo(client.restTemplate).build();
        address = new ResolvedAddress("서울특별시 도봉구 방학동 123-4", 37.66, 127.04,
                "1132010600", "11320", "10600", "0", "0123", "0004", null);
    }

    @Test
    void selectsMainBuildingByOfficialCodeAndTreatsZeroHouseholdsAsMissing() {
        server.expect(once(), requestTo("https://example.test/BldRgstHubService/getBrTitleInfo?serviceKey=encoded/key%3D%3D&sigunguCd=11320&bjdongCd=10600&platGbCd=0&bun=0123&ji=0004&numOfRows=100&pageNo=1&_type=json"))
                .andExpect(method(GET))
                .andRespond(withSuccess("""
                        {"response":{"header":{"resultCode":"00"},"body":{"items":{"item":[
                          {"mgmBldrgstPk":"accessory","mainAtchGbCd":"1","mainAtchGbCdNm":"부속건축물"},
                          {"mgmBldrgstPk":"main","mainAtchGbCd":"0","hhldCnt":"0","useAprDay":"20040618"}
                        ]}}}}
                        """, APPLICATION_JSON));

        BuildingRegisterTitle result = client.findTitle(address).orElseThrow();

        assertThat(result.registerId()).isEqualTo("main");
        assertThat(result.approvalDate()).hasToString("2004-06-18");
        assertThat(result.householdCount()).isNull();
        server.verify();
    }

    @Test
    void readsComplexTotalsFromRecapTitle() {
        server.expect(once(), requestTo("https://example.test/BldRgstHubService/getBrRecapTitleInfo?serviceKey=encoded/key%3D%3D&sigunguCd=11320&bjdongCd=10600&platGbCd=0&bun=0123&ji=0004&numOfRows=100&pageNo=1&_type=json"))
                .andExpect(method(GET))
                .andExpect(queryParam("sigunguCd", "11320"))
                .andExpect(queryParam("bjdongCd", "10600"))
                .andRespond(withSuccess("""
                        {"response":{"header":{"resultCode":"00"},"body":{"items":{"item":{
                          "mgmBldrgstPk":"recap","useAprDay":"20040618","hhldCnt":"720",
                          "mainBldCnt":"8","indrAutoUtcnt":"500","oudrAutoUtcnt":"40"
                        }}}}}
                        """, APPLICATION_JSON));

        BuildingRegisterRecap result = client.findRecap(address).orElseThrow();

        assertThat(result.registerId()).isEqualTo("recap");
        assertThat(result.householdCount()).isEqualTo(720);
        assertThat(result.buildingCount()).isEqualTo(8);
        assertThat(result.parkingCount()).isEqualTo(540);
        server.verify();
    }

    @Test
    void skipsOptionalRecapEnrichmentWhenProviderReturnsAnError() {
        server.expect(once(), requestTo("https://example.test/BldRgstHubService/getBrRecapTitleInfo?serviceKey=encoded/key%3D%3D&sigunguCd=11320&bjdongCd=10600&platGbCd=0&bun=0123&ji=0004&numOfRows=100&pageNo=1&_type=json"))
                .andRespond(withSuccess("""
                        {"response":{"header":{"resultCode":"99","resultMsg":"SERVICE ERROR"}}}
                        """, APPLICATION_JSON));

        assertThat(client.findRecap(address)).isEmpty();
        server.verify();
    }
}
