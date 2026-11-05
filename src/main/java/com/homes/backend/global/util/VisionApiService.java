package com.homes.backend.global.util;

import com.google.api.gax.core.FixedCredentialsProvider;
import com.google.auth.oauth2.GoogleCredentials;
import com.google.cloud.vision.v1.*;
import com.google.protobuf.ByteString;
import com.homes.backend.domain.verification.exception.VerificationErrorCode;
import com.homes.backend.global.exception.CustomException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.io.ByteArrayInputStream;
import java.util.Base64;
import java.util.List;

@Slf4j
@Component
public class VisionApiService {

    @Value("${google.vision.key-base64}")
    private String visionKeyBase64;

    /**
     * 역이미지 검색 (중개사 현장 인증 - 도용 방지용)
     */
    public boolean isStolenImage(byte[] imageBytes) {
        try {
            // 1. Base64 환경변수 디코딩
            byte[] decodedKey = Base64.getDecoder().decode(visionKeyBase64);
            GoogleCredentials credentials = GoogleCredentials.fromStream(new ByteArrayInputStream(decodedKey));

            ImageAnnotatorSettings settings = ImageAnnotatorSettings.newBuilder()
                    .setCredentialsProvider(FixedCredentialsProvider.create(credentials))
                    .build();

            try (ImageAnnotatorClient client = ImageAnnotatorClient.create(settings)) {
                ByteString imgBytes = ByteString.copyFrom(imageBytes);
                Image img = Image.newBuilder().setContent(imgBytes).build();

                Feature feat = Feature.newBuilder().setType(Feature.Type.WEB_DETECTION).build();
                AnnotateImageRequest request = AnnotateImageRequest.newBuilder()
                        .addFeatures(feat)
                        .setImage(img)
                        .build();

                BatchAnnotateImagesResponse response = client.batchAnnotateImages(List.of(request));

                for (AnnotateImageResponse res : response.getResponsesList()) {
                    if (res.hasError()) {
                        log.error("Vision API 에러: {}", res.getError().getMessage());
                        throw new CustomException(VerificationErrorCode.VISION_API_ERROR);
                    }

                    WebDetection webDetection = res.getWebDetection();
                    boolean hasFullMatches = webDetection.getFullMatchingImagesCount() > 0;
                    boolean hasPartialMatches = webDetection.getPartialMatchingImagesCount() > 0;

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
        return false;
    }

    /**
     * OCR 텍스트 추출 (집주인 등기부등본 인증용)
     */
    public String extractTextFromImage(byte[] imageBytes) {
        try {
            // 1. Base64 환경변수 디코딩 (여기에도 동일하게 적용)
            byte[] decodedKey = Base64.getDecoder().decode(visionKeyBase64);
            GoogleCredentials credentials = GoogleCredentials.fromStream(new ByteArrayInputStream(decodedKey));

            ImageAnnotatorSettings settings = ImageAnnotatorSettings.newBuilder()
                    .setCredentialsProvider(FixedCredentialsProvider.create(credentials))
                    .build();

            try (ImageAnnotatorClient client = ImageAnnotatorClient.create(settings)) {
                ByteString imgBytes = ByteString.copyFrom(imageBytes);
                Image img = Image.newBuilder().setContent(imgBytes).build();

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