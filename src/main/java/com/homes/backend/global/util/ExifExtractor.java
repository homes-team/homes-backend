package com.homes.backend.global.util;

import com.drew.imaging.ImageMetadataReader;
import com.drew.metadata.Metadata;
import com.drew.metadata.exif.ExifSubIFDDirectory;
import com.drew.metadata.exif.GpsDirectory;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Date;
import java.util.TimeZone;

@Slf4j
@Component
public class ExifExtractor {
    // 파라미터로 이미 안전하게 다운로드된 바이트(byte[])만 받음
    public ExifData extractExif(byte[] imageBytes) {

        // 메모리에 있는 바이트 배열을 읽기 위한 InputStream 생성
        try (InputStream is = new ByteArrayInputStream(imageBytes)) {
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

        } catch (Exception e) {
            log.error("EXIF 추출 중 오류 발생: {}", e.getMessage());
            return null; // 메타데이터가 없거나 깨진 사진이면 null 반환
        }
    }
}


