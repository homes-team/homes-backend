package com.homes.backend.global.util;

import com.homes.backend.domain.verification.exception.VerificationErrorCode;
import com.homes.backend.global.exception.CustomException;
import org.springframework.stereotype.Component;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URI;
import java.net.URL;

@Component
public class ImageDownloadUtil {
    private static final int MAX_FILE_SIZE = 10 * 1024 * 1024; // 10MB 제한
    private static final int TIMEOUT_MS = 5000; // 5초 타임아웃

    public byte[] downloadImageSecurely(String photoUrl) {
        try {
            // SSRF 방어: URI 파싱 및 호스트 검증 (S3 버킷 도메인만 허용하도록 응용 가능)
            URI uri = new URI(photoUrl);
            String host = uri.getHost();
            if (host == null || host.equals("localhost") || host.startsWith("127.") || host.startsWith("169.254.")) {
                throw new CustomException(VerificationErrorCode.INVALID_IMAGE_URL);
            }

            URL url = uri.toURL();
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();

            // DoS 방어: 타임아웃 설정
            conn.setConnectTimeout(TIMEOUT_MS);
            conn.setReadTimeout(TIMEOUT_MS);
            // 위키피디아 등 403 방어
            conn.setRequestProperty("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/91.0.4472.124 Safari/537.36");

            int responseCode = conn.getResponseCode();
            if (responseCode != HttpURLConnection.HTTP_OK) {
                throw new CustomException(VerificationErrorCode.INVALID_IMAGE_URL);
            }

            // DoS 방어: 스트림 용량 제한 및 바이트 배열로 1회 다운로드
            try (InputStream in = conn.getInputStream();
                 ByteArrayOutputStream out = new ByteArrayOutputStream()) {

                byte[] buffer = new byte[4096];
                int totalBytes = 0;
                int bytesRead;

                while ((bytesRead = in.read(buffer)) != -1) {
                    totalBytes += bytesRead;
                    if (totalBytes > MAX_FILE_SIZE) {
                        throw new CustomException(VerificationErrorCode.INVALID_IMAGE_URL); // 용량 초과 예외
                    }
                    out.write(buffer, 0, bytesRead);
                }
                return out.toByteArray();
            }
        } catch (CustomException e) {
            throw e;
        } catch (Exception e) {
            throw new CustomException(VerificationErrorCode.INVALID_IMAGE_URL);
        }
    }
}
