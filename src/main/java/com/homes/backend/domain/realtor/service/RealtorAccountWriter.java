package com.homes.backend.domain.realtor.service;

import com.homes.backend.domain.realtor.dto.request.RealtorSignupReqDto;
import com.homes.backend.domain.realtor.entity.Agent;
import com.homes.backend.domain.realtor.repository.AgentRepository;
import com.homes.backend.domain.user.entity.User;
import com.homes.backend.domain.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@RequiredArgsConstructor
public class RealtorAccountWriter {

    private final UserRepository userRepository;
    private final AgentRepository agentRepository;

    @Transactional
    public Agent write(
            User user,
            RealtorSignupReqDto request,
            Double officeLatitude,
            Double officeLongitude
    ) {
        User savedUser = userRepository.save(user);

        Agent agent = Agent.builder()
                .user(savedUser)
                .businessNum(request.businessNum())
                .officeName(request.officeName())
                .officeAddress(request.officeAddress())
                .officeLatitude(officeLatitude)
                .officeLongitude(officeLongitude)
                .businessCertUrl(request.businessCertUrl())
                .agentCertUrl(request.agentCertUrl())
                .profileImageUrl(request.profileImageUrl())
                .build();

        return agentRepository.save(agent);
    }
}