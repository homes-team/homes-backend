package com.homes.backend.global.geocoding;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.util.Optional;

/**
 * 카카오 로컬 API(주소 검색)로 주소를 위경도로 변환한다.
 * Property 도메인도 동일한 변환이 필요해서(스펙상 공용 기능으로 명시) global에 둔다.
 */
@Slf4j
@Component
public class GeocodingService {

    @Value("${kakao.rest-api-key}")
    private String kakaoRestApiKey;

    /**
     * 주소를 좌표로 변환. 주소가 없거나, 카카오 API가 결과를 못 찾거나, 통신에 실패하면
     * 예외를 던지지 않고 빈 Optional을 반환한다 - 지오코딩 실패가 회원가입/프로필 수정 자체를 막아서는 안 되기 때문.
     */
    public Optional<GeocodedPoint> geocode(String address) {
        if (!StringUtils.hasText(address)) {
            return Optional.empty();
        }

        // 연결 3초, 응답 5초 제한
        SimpleClientHttpRequestFactory requestFactory = new SimpleClientHttpRequestFactory();
        requestFactory.setConnectTimeout(3000);
        requestFactory.setReadTimeout(5000);

        RestTemplate restTemplate = new RestTemplate(requestFactory);
        HttpHeaders headers = new HttpHeaders();
        headers.set("Authorization", "KakaoAK " + kakaoRestApiKey);

        HttpEntity<Void> httpEntity = new HttpEntity<>(headers);
        // URI 객체로 미리 인코딩해서 넘겨야 한다 - String URL을 그대로 넘기면 RestTemplate이 내부적으로
        // 한 번 더 인코딩해서(이중 인코딩) 한글 등 non-ASCII 쿼리가 깨진 값으로 전송되는 문제가 있다.
        URI uri = UriComponentsBuilder.fromHttpUrl("https://dapi.kakao.com/v2/local/search/address.json")
                .queryParam("query", address)
                .encode(StandardCharsets.UTF_8)
                .build()
                .toUri();

        try {
            ResponseEntity<String> response = restTemplate.exchange(uri, HttpMethod.GET, httpEntity, String.class);
            JsonNode documents = new ObjectMapper().readTree(response.getBody()).path("documents");

            if (!documents.isArray() || documents.isEmpty()) {
                log.warn("카카오 지오코딩 결과 없음: address={}", address);
                return Optional.empty();
            }

            JsonNode first = documents.get(0);
            double longitude = first.path("x").asDouble();
            double latitude = first.path("y").asDouble();
            return Optional.of(new GeocodedPoint(latitude, longitude));
        } catch (Exception e) {
            log.warn("카카오 지오코딩 통신 실패: address={}", address, e);
            return Optional.empty();
        }
    }
}
