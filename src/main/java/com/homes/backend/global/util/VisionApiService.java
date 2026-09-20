package com.homes.backend.global.util;

import com.google.api.gax.core.FixedCredentialsProvider;
import com.google.auth.oauth2.GoogleCredentials;
import com.google.cloud.vision.v1.*;
import com.google.protobuf.ByteString;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Component;

import java.io.InputStream;
import java.net.URL;
import java.util.List;

@Slf4j
@Component
public class VisionApiService {
    public boolean isStolenImage(String imageUrl) {
        try {
            // JSON 키 파일 로드
            InputStream keyStream = new ClassPathResource("google-vision-key.json").getInputStream();
            GoogleCredentials credentials = GoogleCredentials.fromStream(keyStream);

            ImageAnnotatorSettings settings = ImageAnnotatorSettings.newBuilder()
                    .setCredentialsProvider(FixedCredentialsProvider.create(credentials))
                    .build();

            try (ImageAnnotatorClient client = ImageAnnotatorClient.create(settings)) {
                // 이미지 URL에서 바이트 데이터 읽어오기
                ByteString imgBytes;
                try (InputStream in = new URL(imageUrl).openStream()) {
                    imgBytes = ByteString.readFrom(in);
                }

                Image img = Image.newBuilder().setContent(imgBytes).build();
                // 웹 검색 기능 활성화
                Feature feat = Feature.newBuilder().setType(Feature.Type.WEB_DETECTION).build();
                AnnotateImageRequest request = AnnotateImageRequest.newBuilder()
                        .addFeatures(feat)
                        .setImage(img)
                        .build();

                // API 호출
                BatchAnnotateImagesResponse response = client.batchAnnotateImages(List.of(request));

                for (AnnotateImageResponse res : response.getResponsesList()) {
                    if (res.hasError()) {
                        log.error("Vision API 에러: {}", res.getError().getMessage());
                        return false; // 에러 발생 시 일단 통과시킴 (API 장애로 인한 서비스 마비 방지)
                    }

                    // 웹 검색(도용) 결과 분석
                    WebDetection webDetection = res.getWebDetection();
                    boolean hasFullMatches = webDetection.getFullMatchingImagesCount() > 0;
                    boolean hasPartialMatches = webDetection.getPartialMatchingImagesCount() > 0;

                    // 인터넷에 완전히 똑같거나(Full), 잘린 형태(Partial)의 사진이 이미 존재하면 도용!
                    if (hasFullMatches || hasPartialMatches) {
                        log.warn("도용된 사진 발견! URL: {}", imageUrl);
                        return true;
                    }
                }
            }
        } catch (Exception e) {
            log.error("Vision API 연동 중 오류 발생: {}", e.getMessage());
        }
        return false; // 도용 내역이 없으면 false 반환
    }
}
