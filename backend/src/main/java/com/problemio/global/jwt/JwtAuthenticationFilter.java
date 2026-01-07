package com.problemio.global.jwt;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserCache;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

@RequiredArgsConstructor
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private final JwtTokenProvider jwtTokenProvider;
    private final UserDetailsService userDetailsService; // 유저 정보 조회용 (구현 필요)
    private final UserCache userCache;

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain) throws ServletException, IOException {

        // Request Header에서 토큰 추출
        String token = resolveToken(request);

        // 토큰 유효성 검사
        if (token != null
                && jwtTokenProvider.validateToken(token)
                && SecurityContextHolder.getContext().getAuthentication() == null) {

            // 토큰에서 이메일 추출
            String email = jwtTokenProvider.getEmail(token);

            // 캐시 → DB 순으로 UserDetails 조회
            UserDetails cached = userCache.getUserFromCache(email);
            UserDetails userDetails = cached != null
                    ? cached
                    : userDetailsService.loadUserByUsername(email);
            if (cached == null) {
                userCache.putUserInCache(userDetails);
            }

            // 스프링 시큐리티에 인증 정보 저장
            UsernamePasswordAuthenticationToken authentication =
                    new UsernamePasswordAuthenticationToken(userDetails, null, userDetails.getAuthorities());

            SecurityContextHolder.getContext().setAuthentication(authentication);
        }

        // 다음 필터로 넘기기
        filterChain.doFilter(request, response);
    }

    // 헤더에서 "Bearer {토큰}" 꺼내오는 메서드
    private String resolveToken(HttpServletRequest request) {
        String bearerToken = request.getHeader("Authorization");
        if (StringUtils.hasText(bearerToken) && bearerToken.startsWith("Bearer ")) {
            return bearerToken.substring(7);
        }
        return null;
    }
}
