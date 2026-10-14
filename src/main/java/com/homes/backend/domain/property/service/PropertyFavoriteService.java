package com.homes.backend.domain.property.service;

import com.homes.backend.domain.property.entity.Property;
import com.homes.backend.domain.property.entity.PropertyFavorite;
import com.homes.backend.domain.property.exception.PropertyErrorCode;
import com.homes.backend.domain.property.repository.PropertyFavoriteRepository;
import com.homes.backend.domain.property.repository.PropertyRepository;
import com.homes.backend.domain.user.entity.User;
import com.homes.backend.domain.user.exception.UserErrorCode;
import com.homes.backend.domain.user.repository.UserRepository;
import com.homes.backend.global.exception.CustomException;
import com.homes.backend.global.exception.GlobalErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

/**
 * 찜 하기 생성/삭제
 */
@Service
@RequiredArgsConstructor
@Transactional
public class PropertyFavoriteService {

    private final PropertyFavoriteRepository favoriteRepository;
    private final UserRepository userRepository;
    private final PropertyRepository propertyRepository;
    private final PropertyRankingService propertyRankingService;

    public boolean toggleFavorite(Long userId, Long propertyId){
        User user=userRepository.findById(userId)
                .orElseThrow(()->new CustomException(UserErrorCode.USER_NOT_FOUND));
        Property property=propertyRepository.findById(propertyId)
                .orElseThrow(()->new CustomException(PropertyErrorCode.PROPERTY_NOT_FOUND));

        /*
         * 본인 매물인지 검증(본인 매물일시 찜 불가)
         */
        if (property.getUser().getId().equals(userId)) {
            throw new CustomException(PropertyErrorCode.CANNOT_FAVORITE_OWN_PROPERTY);
        }

        /*
         * 토클 로직: 이미 찜 했으면 삭제, 아님 생성
         */
        Optional<PropertyFavorite> existingFavorite = favoriteRepository.findByUserAndProperty(user,property);

        if (existingFavorite.isPresent()){ // 이미 찜한 상태
            favoriteRepository.delete(existingFavorite.get());
            propertyRepository.decreaseFavoriteCount(propertyId);

            /*
             * 찜 취소 시 랭킹 점수 5점 차감
             */
            try {
                propertyRankingService.decrementFavoriteScore(propertyId);
            } catch (Exception e) {
                // Redis 에러가 메인 트랜잭션을 롤백하지 않도록 방어
            }

            return false;
        } else{ // 찜 생성
            try {
                PropertyFavorite newFavorite = PropertyFavorite.builder()
                        .user(user)
                        .property(property)
                        .build();
                favoriteRepository.save(newFavorite);
                favoriteRepository.flush(); //save 직후 DB에 즉시 반영

                propertyRepository.increaseFavoriteCount(propertyId);

                /*
                 * 찜 등록 시 랭킹 점수 5점 증가
                 */
                try {
                    propertyRankingService.incrementFavoriteScore(propertyId);
                } catch (Exception e) {
                    // Redis 에러 방어
                }

                return true;

            } catch (DataIntegrityViolationException e) { // 이미 DB에 찜 기록이 들어가 있는데 또 넣으려고 해서 에러가 난 경우
                throw new CustomException(GlobalErrorCode.BAD_REQUEST);
            }
        }
    }
}
