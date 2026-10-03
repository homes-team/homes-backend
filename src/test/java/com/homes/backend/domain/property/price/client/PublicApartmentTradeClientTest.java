package com.homes.backend.domain.property.price.client;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.homes.backend.domain.property.building.config.PublicDataApiProperties;
import com.homes.backend.domain.property.price.model.ApartmentTrade;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class PublicApartmentTradeClientTest {
    private final PublicApartmentTradeClient client =
            new PublicApartmentTradeClient(new PublicDataApiProperties(), new ObjectMapper());

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
