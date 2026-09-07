package com.san.api.global.security.service;

import com.san.api.domain.user.entity.User;
import com.san.api.domain.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Spring Security 인증 흐름에서 사용자 정보를 로드하는 서비스.
 *
 * JwtAuthenticationFilter에서는 직접 호출하지 않고,
 * FormLogin 혹은 AuthenticationManager 사용 시 간접 호출됩니다.
 */
@Service
@RequiredArgsConstructor
public class CustomUserDetailsService implements UserDetailsService {

    private final UserRepository userRepository;

    @Override
    @Transactional(readOnly = true)
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new UsernameNotFoundException("사용자를 찾을 수 없습니다: " + username));

        return org.springframework.security.core.userdetails.User.builder()
                .username(user.getUserId().toString())
                .password(user.getPasswordHash() != null ? user.getPasswordHash() : "")
                .roles("USER")
                .build();
    }
}
