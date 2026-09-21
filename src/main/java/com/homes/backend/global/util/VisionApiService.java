package com.homes.backend.global.util;

import com.google.api.gax.core.FixedCredentialsProvider;
import com.google.auth.oauth2.GoogleCredentials;
import com.google.cloud.vision.v1.*;
import com.google.protobuf.ByteString;
import com.homes.backend.domain.verification.exception.VerificationErrorCode;
import com.homes.backend.global.exception.CustomException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Component;

import java.io.InputStream;
import java.util.List;

@Slf4j
@Component
public class VisionApiService {
    /**
     * 역이미지 검색 (중개사 현장 인증 - 도용 방지용)
     */
    public boolean isStolenImage(byte[] imageBytes) {
        try {
            // JSON 키 파일 로드
            InputStream keyStream = new ClassPathResource("google-vision-key.json").getInputStream();
            GoogleCredentials credentials = GoogleCredentials.fromStream(keyStream);

            ImageAnnotatorSettings settings = ImageAnnotatorSettings.newBuilder()
                    .setCredentialsProvider(FixedCredentialsProvider.create(credentials))
                    .build();

            try (ImageAnnotatorClient client = ImageAnnotatorClient.create(settings)) {
                ByteString imgBytes = ByteString.copyFrom(imageBytes);

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
                        throw new CustomException(VerificationErrorCode.VISION_API_ERROR);
                    }

                    // 웹 검색(도용) 결과 분석
                    WebDetection webDetection = res.getWebDetection();
                    boolean hasFullMatches = webDetection.getFullMatchingImagesCount() > 0;
                    boolean hasPartialMatches = webDetection.getPartialMatchingImagesCount() > 0;

                    // 인터넷에 완전히 똑같거나(Full), 잘린 형태(Partial)의 사진이 이미 존재하면 도용!
                    if (hasFullMatches || hasPartialMatches) {
                        log.warn("도용된 사진 발견!");
                        return true;
                    }
                }
            }
        } catch (Exception e) {
            log.error("Vision API 연동 중 오류 발생: {}", e.getMessage());
            throw new CustomException(VerificationErrorCode.VISION_API_ERROR);
        }
        return false; // 도용 내역이 없으면 false 반환
    }

    /**
     * OCR 텍스트 추출 (집주인 등기부등본 인증용)
     */
    public String extractTextFromImage(byte[] imageBytes) {
        try {
            InputStream keyStream = new ClassPathResource("google-vision-key.json").getInputStream();
            GoogleCredentials credentials = GoogleCredentials.fromStream(keyStream);

            ImageAnnotatorSettings settings = ImageAnnotatorSettings.newBuilder()
                    .setCredentialsProvider(FixedCredentialsProvider.create(credentials))
                    .build();

            try (ImageAnnotatorClient client = ImageAnnotatorClient.create(settings)) {
                ByteString imgBytes = ByteString.copyFrom(imageBytes);
                Image img = Image.newBuilder().setContent(imgBytes).build();

                // 텍스트 추출
                Feature feat = Feature.newBuilder().setType(Feature.Type.DOCUMENT_TEXT_DETECTION).build();
                AnnotateImageRequest request = AnnotateImageRequest.newBuilder()
                        .addFeatures(feat)
                        .setImage(img)
                        .build();

                BatchAnnotateImagesResponse response = client.batchAnnotateImages(List.of(request));

                for (AnnotateImageResponse res : response.getResponsesList()) {
                    if (res.hasError()) {
                        log.error("Vision OCR 에러: {}", res.getError().getMessage());
                        throw new CustomException(VerificationErrorCode.VISION_API_ERROR);
                    }

                    // 이미지 안의 모든 텍스트를 추출하여 통째로 반환
                    return res.getFullTextAnnotation().getText();
                }
            }
        } catch (CustomException e) {
            throw e;
        } catch (Exception e) {
            log.error("Vision OCR 연동 중 오류 발생: {}", e.getMessage());
            throw new CustomException(VerificationErrorCode.VISION_API_ERROR);
        }
        return "";
    }
}
