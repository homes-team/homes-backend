package com.homes.backend.domain.verification.service;

import com.homes.backend.domain.verification.entity.OwnerVerification;
import com.homes.backend.domain.verification.entity.VerificationStatus;
import com.homes.backend.domain.verification.repository.OwnerVerificationRepository;
import com.homes.backend.global.util.ImageDownloadUtil;
import com.homes.backend.global.util.VisionApiService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Slf4j
@Component
@RequiredArgsConstructor
public class OwnerVerificationWorker {
    private final OwnerVerificationRepository ownerVerificationRepository;
    private final ImageDownloadUtil imageDownloadUtil;
    private final VisionApiService visionApiService;

    @Async
    @Transactional
    public void processOcrVerification(Long verificationId, String documentUrl, String realName) {
        OwnerVerification verification = ownerVerificationRepository.findById(verificationId)
                .orElseThrow();

        try {
            // 보안 다운로더로 이미지 1회 다운로드
            byte[] imageBytes = imageDownloadUtil.downloadImageSecurely(documentUrl);

            // 구글 Vision API로 텍스트 추출
            String extractedText = visionApiService.extractTextFromImage(imageBytes);
            log.info("OCR 텍스트 추출 완료 (길이: {})", extractedText.length());

            // 정규식 이름 대조 로직
            boolean isMatched = checkOwnerNameWithRegex(extractedText, realName);

            if (isMatched) {
                // 성공 (소유자명 위치에서 실명 확인됨)
                verification.completeVerification(realName, true, VerificationStatus.APPROVED, null);
            } else {
                // 실패 (이름이 없거나 소유자가 아님 -> 관리자 수동 검수)
                verification.completeVerification(null, false, VerificationStatus.MANUAL_REVIEW, "소유자명 불일치 또는 OCR 파싱 실패");
            }

            ownerVerificationRepository.save(verification); // 비동기 메서드이므로 명시적 save 권장

        } catch (Exception e) {
            log.error("OCR 처리 중 오류 발생. 수동 검수로 전환합니다: {}", e.getMessage());
            verification.completeVerification(null, false, VerificationStatus.MANUAL_REVIEW, "OCR 통신/시스템 에러");
            ownerVerificationRepository.save(verification);
        }
    }

    /**
     * OCR 텍스트에서 '소유자' 또는 '공유자' 이름이 실명과 일치하는지 정규식으로 검증
     */
    private boolean checkOwnerNameWithRegex(String ocrText, String realName) {
        if (ocrText == null || realName == null) return false;

        // 띄어쓰기 및 줄바꿈을 모두 제거하여 변칙적인 OCR 공백 문제 해결
        String compressedText = ocrText.replaceAll("\\s+", "");
        String compressedName = realName.replaceAll("\\s+", "");

        // 정규식 패턴: '소유자' 또는 '공유자'라는 단어 뒤에 최대 5글자(특수기호 등) 이내에 '실명'이 등장하는지 검사
        // 매칭 예시: "소유자ooo", "소유자:ooo", "공유자(지분)ooo" 등
        String regex = "(소유자|공유자).{0,5}?" + compressedName;

        Pattern pattern = Pattern.compile(regex);
        Matcher matcher = pattern.matcher(compressedText);

        return matcher.find();
    }
}

