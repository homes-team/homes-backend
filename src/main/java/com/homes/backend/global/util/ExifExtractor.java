package com.homes.backend.global.util;

import com.drew.imaging.ImageMetadataReader;
import com.drew.metadata.Metadata;
import com.drew.metadata.exif.ExifSubIFDDirectory;
import com.drew.metadata.exif.GpsDirectory;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Date;
import java.util.TimeZone;

@Slf4j
@Component
public class ExifExtractor {
    //  DTO에 촬영 시각 추가
    public ExifData extractExif(String imageUrl) {
        // SSRF 방어: 허용된 프로토콜 및 호스트만 접근 허용
        if (!imageUrl.startsWith("https://") && !imageUrl.startsWith("http://localhost")) {
            log.warn("SSRF 차단 - 허용되지 않은 URL 프로토콜: {}", imageUrl);
            return null;
        }
        // TODO: 실제 운영 시 S3 버킷 도메인 검증 추가(배포 완료 후에 작업 진행)
        // if (!imageUrl.contains("s3.ap-northeast-2.amazonaws.com")) { return null; }

        HttpURLConnection connection = null;
        try {
            URL url = new URL(imageUrl);
            connection = (HttpURLConnection) url.openConnection();

            // 무한 대기 방지를 위한 타임아웃 설정 (연결 3초, 읽기 5초)
            connection.setConnectTimeout(3000);
            connection.setReadTimeout(5000);
            connection.setRequestMethod("GET");

            try (InputStream is = connection.getInputStream()) {
                Metadata metadata = ImageMetadataReader.readMetadata(is);

                // GPS 추출
                GpsDirectory gpsDir = metadata.getFirstDirectoryOfType(GpsDirectory.class);
                if (gpsDir == null || gpsDir.getGeoLocation() == null) {
                    return null;
                }
                double lat = gpsDir.getGeoLocation().getLatitude();
                double lon = gpsDir.getGeoLocation().getLongitude();

                // 촬영 시각 추출 (과거 사진 재탕 방지)
                ExifSubIFDDirectory subIFDDirectory = metadata.getFirstDirectoryOfType(ExifSubIFDDirectory.class);
                LocalDateTime originalDate = null;

                if (subIFDDirectory != null) {
                    Date date = subIFDDirectory.getDateOriginal(TimeZone.getTimeZone("Asia/Seoul"));
                    if (date != null) {
                        originalDate = date.toInstant().atZone(ZoneId.of("Asia/Seoul")).toLocalDateTime();
                    }
                }

                return new ExifData(lat, lon, originalDate);
            }
        } catch (Exception e) {
            log.error("EXIF 추출 중 오류 발생: {}", e.getMessage());
            return null;
        } finally {
            if (connection != null) {
                connection.disconnect(); // 자원 해제
            }
        }
    }
}


