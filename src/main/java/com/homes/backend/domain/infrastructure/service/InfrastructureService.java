package com.homes.backend.domain.infrastructure.service;

import com.homes.backend.domain.infrastructure.dto.response.InfrastructureResDto;
import com.homes.backend.domain.infrastructure.entity.InfraType;
import com.homes.backend.domain.infrastructure.repository.InfrastructureRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class InfrastructureService {

    private final InfrastructureRepository infrastructureRepository;

    public List<InfrastructureResDto> getInfrastructuresInBoundingBox(
            Double minLat, Double minLon, Double maxLat, Double maxLon, InfraType infraType) {

        // Enum 값을 String으로 변환. 안 넘어왔으면 null 유지
        String typeStr = (infraType != null) ? infraType.name() : null;

        return infrastructureRepository.findInBoundingBox(minLon, minLat, maxLon, maxLat, typeStr)
                .stream()
                .map(InfrastructureResDto::from)
                .toList();
    }
}
