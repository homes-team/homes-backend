package com.homes.backend.domain.user.service;

import com.homes.backend.domain.chat.entity.ChatRoom;
import com.homes.backend.domain.chat.exception.ChatErrorCode;
import com.homes.backend.domain.chat.repository.ChatRoomRepository;
import com.homes.backend.domain.user.dto.request.UserReportCreateReqDto;
import com.homes.backend.domain.user.entity.User;
import com.homes.backend.domain.user.entity.UserReport;
import com.homes.backend.domain.user.entity.UserReportReason;
import com.homes.backend.domain.user.exception.UserErrorCode;
import com.homes.backend.domain.user.repository.UserReportRepository;
import com.homes.backend.domain.user.repository.UserRepository;
import com.homes.backend.global.exception.CustomException;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional
public class UserReportService {

    private final UserReportRepository userReportRepository;
    private final UserRepository userRepository;
    private final ChatRoomRepository chatRoomRepository;

    /**
     * 유저 신고 접수. 실제로 채팅해본 적 있는 상대만 신고할 수 있게 막아, 아무나 임의로 신고하는 걸 방지한다.
     */
    public void createReport(Long reporterId, Long reportedUserId, UserReportCreateReqDto reqDto) {
        if (reporterId.equals(reportedUserId)) {
            throw new CustomException(UserErrorCode.CANNOT_REPORT_SELF);
        }

        User reporter = userRepository.findById(reporterId)
                .orElseThrow(() -> new CustomException(UserErrorCode.USER_NOT_FOUND));
        User reportedUser = userRepository.findById(reportedUserId)
                .orElseThrow(() -> new CustomException(UserErrorCode.USER_NOT_FOUND));

        if (userReportRepository.existsByReportedUserAndReporter(reportedUser, reporter)) {
            throw new CustomException(UserErrorCode.ALREADY_REPORTED_USER);
        }

        if (!chatRoomRepository.existsRoomBetween(reporterId, reportedUserId)) {
            throw new CustomException(UserErrorCode.CANNOT_REPORT_UNCONTACTED_USER);
        }

        ChatRoom chatRoom = null;
        if (reqDto.chatRoomId() != null) {
            chatRoom = chatRoomRepository.findById(reqDto.chatRoomId())
                    .orElseThrow(() -> new CustomException(ChatErrorCode.CHAT_ROOM_NOT_FOUND));

            if (!chatRoom.isMember(reporterId) || !chatRoom.isMember(reportedUserId)) {
                throw new CustomException(ChatErrorCode.NOT_CHAT_MEMBER);
            }
        }

        /**
         * 기타 사유(OTHER)일 때 사용
         */
        String savedCustomReason = null;
        if (reqDto.reason() == UserReportReason.OTHER) {
            savedCustomReason = reqDto.customReason();
        }

        try {
            UserReport report = UserReport.builder()
                    .reason(reqDto.reason())
                    .customReason(savedCustomReason)
                    .reportedUser(reportedUser)
                    .reporter(reporter)
                    .chatRoom(chatRoom)
                    .build();
            userReportRepository.save(report);

            userReportRepository.flush();

            userRepository.increaseReportCountAndCheckSuspicious(reportedUserId);
        } catch (DataIntegrityViolationException e) {
            throw new CustomException(UserErrorCode.ALREADY_REPORTED_USER);
        }
    }
}
