package com.problemio.global.auth;

import com.problemio.user.domain.User;
import com.problemio.user.mapper.UserAuthMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Slf4j
public class CustomUserDetailsService implements UserDetailsService {

    private final UserAuthMapper userAuthMapper;

    @Override
    @Transactional(readOnly = true)
    @Cacheable(cacheNames = "userDetails", key = "#email", sync = true)
    public UserDetails loadUserByUsername(String email) throws UsernameNotFoundException {
        // @Cacheable 적용으로 이 메서드가 실행되면 곧바로 캐시 미스임
        log.info("[UserDetails] cache MISS for email={}", email);

        // DB에서 조회
        User user = userAuthMapper.findByEmail(email)
                .orElseThrow(() -> new UsernameNotFoundException("해당 이메일을 가진 사용자를 찾을 수 없습니다: " + email));

        // UserDetails로 변환하여 반환
        return new CustomUserDetails(user);
    }
}
