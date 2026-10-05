package com.homes.backend.domain.property.price.client;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.homes.backend.domain.property.building.config.PublicDataApiProperties;
import com.homes.backend.domain.property.price.model.ApartmentTrade;
import org.junit.jupiter.api.Test;
import com.github.benmanes.caffeine.cache.Cache;
import org.springframework.http.MediaType;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestTemplate;
import java.time.Duration;
import java.time.YearMonth;
import java.util.concurrent.atomic.AtomicLong;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.anything;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class PublicApartmentTradeClientTest {
    private final PublicApartmentTradeClient client =
            new PublicApartmentTradeClient(new PublicDataApiProperties(), new ObjectMapper());

    @Test
    @SuppressWarnings("unchecked")
    void evictsExpiredTradesAndFetchesThemAgainWithoutExtendingTtlOnReads() {
        AtomicLong nanos = new AtomicLong();
        PublicDataApiProperties properties = new PublicDataApiProperties();
        properties.setServiceKey("test-key");
        PublicApartmentTradeClient cachedClient = new PublicApartmentTradeClient(properties, new ObjectMapper(), nanos::get);
        RestTemplate restTemplate = (RestTemplate) ReflectionTestUtils.getField(cachedClient, "restTemplate");
        MockRestServiceServer server = MockRestServiceServer.bindTo(restTemplate).build();
        String body = "{\"response\":{\"body\":{\"items\":\"\"}}}";
        server.expect(anything()).andRespond(withSuccess(body, MediaType.APPLICATION_JSON));
        server.expect(anything()).andRespond(withSuccess(body, MediaType.APPLICATION_JSON));
        YearMonth month = YearMonth.of(2026, 9);
        cachedClient.findMonthlyTrades("11680", month);
        nanos.set(Duration.ofHours(23).toNanos());
        cachedClient.findMonthlyTrades("11680", month);

        Cache<String, List<ApartmentTrade>> cache =
                (Cache<String, List<ApartmentTrade>>) ReflectionTestUtils.getField(cachedClient, "cache");
        nanos.set(Duration.ofHours(24).toNanos());
        cache.cleanUp();
        assertThat(cache.estimatedSize()).isZero();
        cachedClient.findMonthlyTrades("11680", month);
        server.verify();
    }

    @Test
    @SuppressWarnings("unchecked")
    void evictsExcessDistrictMonthKeys() {
        Cache<String, List<ApartmentTrade>> cache =
                (Cache<String, List<ApartmentTrade>>) ReflectionTestUtils.getField(client, "cache");
        for (int i = 0; i < 1100; i++) {
            cache.put("district:" + i, List.of());
        }
        cache.cleanUp();
        assertThat(cache.estimatedSize()).isEqualTo(1000);
    }

    @Test
    void parsesCurrentPublicDataJsonFields() throws Exception {
        String response = """
                {"response":{"header":{"resultCode":"000"},"body":{"items":{"item":[
                  {"aptNm":"홈즈아파트","umdNm":"역삼동","jibun":"123","excluUseAr":"84.95",
                   "floor":"12","buildYear":"2015","dealAmount":"123,000",
                   "dealYear":"2026","dealMonth":"9","dealDay":"17"}
                ]}}}}
                """;

        List<ApartmentTrade> result = client.parse(response);

        assertThat(result).singleElement().satisfies(trade -> {
            assertThat(trade.apartmentName()).isEqualTo("홈즈아파트");
            assertThat(trade.areaSquareMeters()).isEqualTo(84.95);
            assertThat(trade.priceTenThousandWon()).isEqualTo(123_000);
            assertThat(trade.contractDate()).isEqualTo(LocalDate.of(2026, 9, 17));
        });
    }

    @Test
    void supportsSingleItemObjectAndSkipsMalformedItem() throws Exception {
        String valid = """
                {"response":{"header":{"resultCode":"00"},"body":{"items":{"item":
                  {"aptNm":"A","excluUseAr":"59.9","dealAmount":"70,000","dealYear":"2026","dealMonth":"8","dealDay":"1"}
                }}}}
                """;
        String malformed = """
                {"response":{"header":{"resultCode":"00"},"body":{"items":{"item":[{"aptNm":"A"}]}}}}
                """;

        assertThat(client.parse(valid)).hasSize(1);
        assertThat(client.parse(malformed)).isEmpty();
    }

    @Test
    void parsesOfficialXmlResponseFormat() throws Exception {
        String response = """
                <response><header><resultCode>000</resultCode></header><body><items><item>
                  <aptNm>홈즈아파트</aptNm><umdNm>역삼동</umdNm><jibun>123</jibun><excluUseAr>84.95</excluUseAr>
                  <floor>12</floor><buildYear>2015</buildYear><dealAmount>123,000</dealAmount>
                  <dealYear>2026</dealYear><dealMonth>9</dealMonth><dealDay>17</dealDay>
                </item><item>
                  <aptNm>취소된거래</aptNm><excluUseAr>84.95</excluUseAr><dealAmount>999,000</dealAmount>
                  <dealYear>2026</dealYear><dealMonth>9</dealMonth><dealDay>18</dealDay><cdealType>O</cdealType>
                </item></items></body></response>
                """;

        assertThat(client.parse(response)).singleElement().satisfies(trade -> {
            assertThat(trade.apartmentName()).isEqualTo("홈즈아파트");
            assertThat(trade.priceTenThousandWon()).isEqualTo(123_000);
            assertThat(trade.contractDate()).isEqualTo(LocalDate.of(2026, 9, 17));
        });
    }

    @Test
    void returnsEmptyListWhenProviderHasNoItems() throws Exception {
        String response = """
                {"response":{"header":{"resultCode":"00"},"body":{"items":""}}}
                """;

        assertThat(client.parse(response)).isEmpty();
    }
}
