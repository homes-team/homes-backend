package com.homes.backend.domain.property.service;

import com.homes.backend.domain.property.dto.response.PropertyListRespDto;
import com.homes.backend.domain.property.entity.PropertyStatus;
import com.homes.backend.domain.property.repository.PropertyRepository;
import com.homes.backend.domain.property.repository.RecentViewRepository;
import com.homes.backend.domain.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional
public class RecentViewService {
    private final RecentViewRepository recentViewRepository;
    private final UserRepository userRepository;
    private final PropertyRepository propertyRepository;

    /**
     * 최근 본 방 기록 추가 및 갱신 (Upsert)
     */
    public void addRecentView(Long userId, Long propertyId) {
        recentViewRepository.upsertRecentView(userId, propertyId);
    }

    /**
     * 최근 본 방 목록 조회
     */
    @Transactional(readOnly = true)
    public List<PropertyListRespDto> getMyRecentViews(Long userId) {
        return recentViewRepository.findTop20ByUserIdAndPropertyStatusNotOrderByViewedAtDesc(userId, PropertyStatus.DELETED).stream()
                .map(recentView -> PropertyListRespDto.from(recentView.getProperty()))
                .toList();
    }
}
