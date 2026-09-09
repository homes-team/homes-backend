package com.homes.backend.global.storage;

import com.homes.backend.global.exception.CustomException;
import com.homes.backend.global.exception.GlobalErrorCode;
import jakarta.annotation.PreDestroy;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.data.redis.core.script.RedisScript;
import org.springframework.stereotype.Component;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.DeleteObjectRequest;
import software.amazon.awssdk.services.s3.model.HeadObjectRequest;
import software.amazon.awssdk.services.s3.model.HeadObjectResponse;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.services.s3.model.S3Exception;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;
import software.amazon.awssdk.services.s3.presigner.model.PresignedPutObjectRequest;
import software.amazon.awssdk.services.s3.presigner.model.PutObjectPresignRequest;

import java.time.Duration;
import java.util.Collections;
import java.util.UUID;

/**
 * 클라이언트가 S3에 직접 파일을 업로드할 수 있는 임시 권한 URL(Presigned URL)을 발급한다.
 * 매물뿐 아니라 다른 도메인(중개사 서류 등)에서도 재사용할 수 있게 global에 둔다.
 */
@Component
public class S3PresignedUrlService {

    private static final Duration UPLOAD_URL_VALID_DURATION = Duration.ofMinutes(5);
    private static final long MAX_UPLOAD_SIZE_BYTES = 10L * 1024 * 1024; // 10MB

    // 발급받은 사람과 실제 제출한 사람이 같은지 대조하기 위한 기록의 유효 기간 (폼 작성 시간 감안)
    private static final Duration ISSUED_KEY_RECORD_TTL = Duration.ofMinutes(30);
    private static final String ISSUED_KEY_PREFIX = "PRESIGN_ISSUED:";

    // 저장된 값이 identity와 같을 때만 원자적으로 삭제한다 (동시 요청에서도 최대 1번만 소비되도록 GET+DEL을 하나의 명령으로 처리)
    private static final RedisScript<Long> COMPARE_AND_DELETE_SCRIPT = new DefaultRedisScript<>(
            "if redis.call('get', KEYS[1]) == ARGV[1] then return redis.call('del', KEYS[1]) else return 0 end",
            Long.class
    );

    private final S3Presigner presigner;
    private final S3Client s3Client;
    private final RedisTemplate<String, Object> redisTemplate;
    private final String bucket;
    private final String publicUrlPrefix;

    public S3PresignedUrlService(
            @Value("${aws.s3.bucket}") String bucket,
            @Value("${aws.s3.region}") String region,
            @Value("${aws.credentials.access-key}") String accessKey,
            @Value("${aws.credentials.secret-key}") String secretKey,
            RedisTemplate<String, Object> redisTemplate
    ) {
        this.redisTemplate = redisTemplate;
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
     * identity: 발급받은 주체 식별자 ("user:{userId}" 또는 "email:{인증된 이메일}") - 나중에 제출 시 대조용으로 기록해둔다
     */
    public PresignedUploadInfo issueUploadUrl(String folder, String originalFileName, String identity) {
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

        redisTemplate.opsForValue().set(ISSUED_KEY_PREFIX + key, identity, ISSUED_KEY_RECORD_TTL);

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
     * 클라이언트가 presigned URL로 이미 업로드를 마친 객체를 실제 저장(회원가입/매물등록 등) 직전에 검증한다.
     * 1) 용량 확인 (10MB 초과 시 즉시 삭제)
     * 2) 이 객체 키가 "발급받은 주체(identity)"와 "지금 제출한 주체"가 일치하는지 대조
     * 통과하면 발급 기록을 삭제해 같은 URL을 재사용(소비)하지 못하게 막는다.
     */
    public void validateAndConsumeUploadedFile(String fileUrl, String identity) {
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
        } catch (S3Exception e) {
            // HeadObject는 응답 바디가 없어 SDK가 NoSuchKeyException 대신 일반 S3Exception(404)으로 던질 수도 있다
            if (e.statusCode() == 404) {
                throw new CustomException(GlobalErrorCode.INVALID_INPUT);
            }
            throw e;
        }

        if (headObjectResponse.contentLength() != null && headObjectResponse.contentLength() > MAX_UPLOAD_SIZE_BYTES) {
            s3Client.deleteObject(DeleteObjectRequest.builder().bucket(bucket).key(key).build());
            throw new CustomException(GlobalErrorCode.FILE_TOO_LARGE);
        }

        String issuedKeyRecord = ISSUED_KEY_PREFIX + key;
        Long deletedCount = redisTemplate.execute(
                COMPARE_AND_DELETE_SCRIPT,
                Collections.singletonList(issuedKeyRecord),
                identity
        );

        // 원자적 compare-and-delete가 실패했다면(기록 없음/만료/소비됨/identity 불일치) 거부
        if (deletedCount == null || deletedCount == 0L) {
            throw new CustomException(GlobalErrorCode.INVALID_INPUT);
        }
    }

    @PreDestroy
    public void close() {
        presigner.close();
        s3Client.close();
    }
}
