package com.homes.backend.global.storage;

import com.homes.backend.global.exception.CustomException;
import com.homes.backend.global.exception.GlobalErrorCode;
import jakarta.annotation.PreDestroy;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.DeleteObjectRequest;
import software.amazon.awssdk.services.s3.model.HeadObjectRequest;
import software.amazon.awssdk.services.s3.model.HeadObjectResponse;
import software.amazon.awssdk.services.s3.model.NoSuchKeyException;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;
import software.amazon.awssdk.services.s3.presigner.model.PresignedPutObjectRequest;
import software.amazon.awssdk.services.s3.presigner.model.PutObjectPresignRequest;

import java.time.Duration;
import java.util.UUID;

/**
 * 클라이언트가 S3에 직접 파일을 업로드할 수 있는 임시 권한 URL(Presigned URL)을 발급한다.
 * 매물뿐 아니라 다른 도메인(중개사 서류 등)에서도 재사용할 수 있게 global에 둔다.
 */
@Component
public class S3PresignedUrlService {

    private static final Duration UPLOAD_URL_VALID_DURATION = Duration.ofMinutes(5);
    private static final long MAX_UPLOAD_SIZE_BYTES = 10L * 1024 * 1024; // 10MB

    private final S3Presigner presigner;
    private final S3Client s3Client;
    private final String bucket;
    private final String publicUrlPrefix;

    public S3PresignedUrlService(
            @Value("${aws.s3.bucket}") String bucket,
            @Value("${aws.s3.region}") String region,
            @Value("${aws.credentials.access-key}") String accessKey,
            @Value("${aws.credentials.secret-key}") String secretKey
    ) {
        this.bucket = bucket;
        this.publicUrlPrefix = "https://" + bucket + ".s3." + region + ".amazonaws.com/";
        AwsBasicCredentials credentials = AwsBasicCredentials.create(accessKey, secretKey);
        this.presigner = S3Presigner.builder()
                .region(Region.of(region))
                .credentialsProvider(StaticCredentialsProvider.create(credentials))
                .build();
        this.s3Client = S3Client.builder()
                .region(Region.of(region))
                .credentialsProvider(StaticCredentialsProvider.create(credentials))
                .build();
    }

    /**
     * folder/{UUID}.{확장자} 형태의 유니크한 키로 업로드 URL을 발급한다.
     * uploadUrl: 클라이언트가 PUT으로 파일 바이트를 쏠 임시 서명 URL (5분간 유효)
     * fileUrl: 업로드가 끝난 뒤 실제로 파일이 저장될 최종 공개 URL
     */
    public PresignedUploadInfo issueUploadUrl(String folder, String originalFileName) {
        String key = folder + "/" + UUID.randomUUID() + extractExtension(originalFileName);

        PutObjectRequest objectRequest = PutObjectRequest.builder()
                .bucket(bucket)
                .key(key)
                .build();

        PutObjectPresignRequest presignRequest = PutObjectPresignRequest.builder()
                .signatureDuration(UPLOAD_URL_VALID_DURATION)
                .putObjectRequest(objectRequest)
                .build();

        PresignedPutObjectRequest presignedRequest = presigner.presignPutObject(presignRequest);

        return new PresignedUploadInfo(presignedRequest.url().toString(), publicUrlPrefix + key);
    }

    private String extractExtension(String fileName) {
        if (fileName == null) {
            return "";
        }
        int dotIndex = fileName.lastIndexOf('.');
        return dotIndex >= 0 ? fileName.substring(dotIndex) : "";
    }

    /**
     * 클라이언트가 presigned URL로 이미 업로드를 마친 객체의 실제 용량을 확인한다.
     * PUT 방식은 사전에 크기를 제한할 수 없어 업로드가 끝난 뒤 검사하고, 초과 시 즉시 삭제한다.
     */
    public void validateUploadedFileSize(String fileUrl) {
        if (fileUrl == null || !fileUrl.startsWith(publicUrlPrefix)) {
            throw new CustomException(GlobalErrorCode.INVALID_INPUT);
        }

        String key = fileUrl.substring(publicUrlPrefix.length());

        HeadObjectResponse headObjectResponse;
        try {
            headObjectResponse = s3Client.headObject(HeadObjectRequest.builder()
                    .bucket(bucket)
                    .key(key)
                    .build());
        } catch (NoSuchKeyException e) {
            throw new CustomException(GlobalErrorCode.INVALID_INPUT);
        }

        if (headObjectResponse.contentLength() != null && headObjectResponse.contentLength() > MAX_UPLOAD_SIZE_BYTES) {
            s3Client.deleteObject(DeleteObjectRequest.builder().bucket(bucket).key(key).build());
            throw new CustomException(GlobalErrorCode.FILE_TOO_LARGE);
        }
    }

    @PreDestroy
    public void close() {
        presigner.close();
        s3Client.close();
    }
}
