package com.homes.backend.domain.admin.service;

import com.homes.backend.domain.admin.dto.response.AdminRealtorDetailResDto;
import com.homes.backend.domain.admin.dto.response.AdminRealtorSummaryResDto;
import com.homes.backend.domain.realtor.entity.Agent;
import com.homes.backend.domain.realtor.exception.RealtorErrorCode;
import com.homes.backend.domain.realtor.repository.AgentRepository;
import com.homes.backend.global.exception.CustomException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class AdminService {

    private final AgentRepository agentRepository;

    public List<AdminRealtorSummaryResDto> getRealtors() {
        return agentRepository.findByIsVerifiedFalseOrderByCreatedAtAsc().stream()
                .map(AdminRealtorSummaryResDto::from)
                .toList();
    }

    public AdminRealtorDetailResDto getRealtorDetail(Long realtorId) {
        Agent agent = agentRepository.findById(realtorId)
                .orElseThrow(() -> new CustomException(RealtorErrorCode.AGENT_NOT_FOUND));

        return AdminRealtorDetailResDto.from(agent);
    }

    @Transactional
    public void approveRealtor(Long realtorId) {
        int updated = agentRepository.approve(realtorId);
        if (updated == 0) {
            throw new CustomException(RealtorErrorCode.AGENT_NOT_FOUND);
        }
    }
}
