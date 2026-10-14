package com.homes.backend.domain.admin.service;

import com.homes.backend.domain.admin.dto.response.AdminReportedUserResDto;
import com.homes.backend.domain.admin.dto.response.AdminUserReportDetailResDto;
import com.homes.backend.domain.user.exception.UserErrorCode;
import com.homes.backend.domain.user.repository.UserReportRepository;
import com.homes.backend.domain.user.repository.UserRepository;
import com.homes.backend.domain.user.service.UserService;
import com.homes.backend.global.exception.CustomException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class AdminUserService {

    private final UserService userService;
    private final UserRepository userRepository;
    private final UserReportRepository userReportRepository;

    /**
     * 악성 유저 강제 탈퇴. 익명화/부수 데이터 정리 로직은 본인 탈퇴와 동일하게 UserService에 있는 걸 재사용한다.
     */
    @Transactional
    public void withdrawUser(Long userId, String reason) {
        userService.forceWithdraw(userId, reason);
    }

    /**
     * 신고 1건 이상 유저 목록. 신고 많은 순 정렬(의심 유저가 자연히 위쪽에 몰림), 이미 탈퇴 처리된 유저는 제외
     */
    public List<AdminReportedUserResDto> getReportedUsers() {
        return userRepository.findByReportCountGreaterThanAndDeletedAtIsNullOrderByReportCountDesc(0).stream()
                .map(AdminReportedUserResDto::from)
                .toList();
    }

    /**
     * 특정 유저의 신고 상세 내역. 의심 유저 여부, 탈퇴 여부와 무관하게 조회 가능(감사 목적)
     */
    public List<AdminUserReportDetailResDto> getUserReportDetail(Long userId) {
        if (!userRepository.existsById(userId)) {
            throw new CustomException(UserErrorCode.USER_NOT_FOUND);
        }

        return userReportRepository.findAllByReportedUserIdOrderByCreatedAtDesc(userId).stream()
                .map(AdminUserReportDetailResDto::from)
                .toList();
    }
}
