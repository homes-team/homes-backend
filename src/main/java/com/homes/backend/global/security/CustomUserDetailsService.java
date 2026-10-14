package com.homes.backend.global.security;

import com.homes.backend.domain.user.entity.User;
import com.homes.backend.domain.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class CustomUserDetailsService implements UserDetailsService {

    private final UserRepository userRepository;

    @Override
    public UserDetails loadUserByUsername(String email) throws UsernameNotFoundException {
        // 부모 메서드 이름은 Username이지만, 우리 서비스는 '이메일'로 유저를 찾음
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new UsernameNotFoundException("해당 이메일을 가진 유저를 찾을 수 없습니다: " + email));

        // 같은 global.security 폴더에 있는 UserPrincipal 상자에 담아 리턴
        return new UserPrincipal(user);
    }
}