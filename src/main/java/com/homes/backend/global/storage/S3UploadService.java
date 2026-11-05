package com.homes.backend.global.storage;

import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;

import java.util.UUID;

@Service
@RequiredArgsConstructor
public class S3UploadService {

    private final S3Client s3Client; // 기존 S3PresignedUrlService에서 쓰는 v2 S3Client 주입

    @Value("${aws.s3.bucket}")
    private String bucket;

    @Value("${aws.s3.region}")
    private String region;

    /**
     * 검증이 완료된 이미지 byte[]를 안전한 격리 폴더에 새 이름으로 직접 업로드
     */
    public String uploadVerifiedImage(byte[] imageBytes, String originalUrl) {
        // 기존 URL에서 확장자 추출 (예: .jpg)
        String extension = ".jpg";
        if (originalUrl != null && originalUrl.lastIndexOf(".") > 0) {
            extension = originalUrl.substring(originalUrl.lastIndexOf("."));
        }

        // 덮어쓰기 불가능한 고유한 새 파일명 생성 (격리 폴더)
        String newFileName = "verified-verifications/" + UUID.randomUUID() + extension;

        // 메타데이터 및 요청 객체 설정
        PutObjectRequest putObjectRequest = PutObjectRequest.builder()
                .bucket(bucket)
                .key(newFileName)
                .contentType("image/jpeg")
                .contentLength((long) imageBytes.length)
                .build();

        // 백엔드에서 직접 S3로 업로드
        s3Client.putObject(putObjectRequest, RequestBody.fromBytes(imageBytes));

        // 새로 생성된 안전한 공개 URL 조합 및 반환
        return "https://" + bucket + ".s3." + region + ".amazonaws.com/" + newFileName;
    }
}
