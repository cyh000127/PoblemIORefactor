package com.problemio.global.config;

import com.problemio.global.jwt.JwtAuthenticationFilter;
import com.problemio.global.jwt.JwtTokenProvider;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.core.userdetails.UserCache;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.cache.SpringCacheBasedUserCache;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

import java.util.Objects;

@Configuration
@EnableWebSecurity
public class SecurityConfig {

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public org.springframework.web.cors.CorsConfigurationSource corsConfigurationSource() {
        org.springframework.web.cors.CorsConfiguration configuration = new org.springframework.web.cors.CorsConfiguration();
        // HTTPS 및 로컬 개발 환경 CORS 허용
        configuration.setAllowedOriginPatterns(java.util.Arrays.asList(
            "https://problemio.cloud", 
            "https://www.problemio.cloud", 
            "http://localhost:5173", 
            "http://localhost:8080"
        ));
        configuration.setAllowedMethods(java.util.Arrays.asList("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS"));
        configuration.setAllowedHeaders(java.util.Arrays.asList("*"));
        configuration.setAllowCredentials(true); // 인증 정보(쿠키 등) 포함 허용

        org.springframework.web.cors.UrlBasedCorsConfigurationSource source = new org.springframework.web.cors.UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", configuration);
        return source;
    }

    @Bean
    public UserCache userCache(CacheManager cacheManager) {
        Cache cache = Objects.requireNonNull(cacheManager.getCache("userDetails"), "userDetails cache not found");
        return new SpringCacheBasedUserCache(cache);
    }

    @Bean
    public JwtAuthenticationFilter jwtAuthenticationFilter(
            JwtTokenProvider jwtTokenProvider,
            UserDetailsService userDetailsService,
            UserCache userCache
    ) {
        return new JwtAuthenticationFilter(jwtTokenProvider, userDetailsService, userCache);
    }

    @Bean
    public SecurityFilterChain securityFilterChain(
            HttpSecurity http,
            UserDetailsService userDetailsService,
            JwtAuthenticationFilter jwtAuthenticationFilter
    ) throws Exception {

        http
                .cors(Customizer.withDefaults())
                .csrf(csrf -> csrf.disable())
                .authorizeHttpRequests(auth -> auth
                        // 1. 인증 엔드포인트 (로그인, 회원가입)
                        .requestMatchers("/api/auth/signup", "/api/auth/login", "/api/auth/reissue").permitAll()
                        .requestMatchers("/api/auth/email/**").permitAll() // 이메일 인증
                        
                        // [Front] 프론트엔드 정적 리소스
                        .requestMatchers("/", "/index.html", "/assets/**", "/*.ico", "/*.png", "/*.svg").permitAll()

                        // [Admin] 관리자 페이지
                        .requestMatchers("/api/admin/**").hasRole("ADMIN")

                        // 2. 정적 파일 접근
                        .requestMatchers("/uploads/**").permitAll()
                        .requestMatchers("/theme/**").permitAll()    
                        .requestMatchers("/popover/**").permitAll()  
                        .requestMatchers("/avatar/**").permitAll()   
                        .requestMatchers(HttpMethod.POST, "/api/files/**").authenticated()

                        // 3. 댓글 (조회/작성/삭제 허용, 좋아요는 인증 필요)
                        .requestMatchers(HttpMethod.GET, "/api/quizzes/*/comments").permitAll()
                        .requestMatchers(HttpMethod.GET, "/api/comments/*/replies").permitAll()
                        .requestMatchers(HttpMethod.POST, "/api/quizzes/*/comments").permitAll()
                        .requestMatchers(HttpMethod.PATCH, "/api/comments/**").permitAll()
                        .requestMatchers(HttpMethod.DELETE, "/api/comments/**").permitAll()
                        .requestMatchers(HttpMethod.POST, "/api/comments/*/likes").authenticated()

                        // 4. 공개 퀴즈 및 제출 (누구나 가능)
                        .requestMatchers(HttpMethod.GET, "/api/quizzes/**").permitAll()
                        .requestMatchers(HttpMethod.POST, "/api/quizzes/*/submissions").permitAll()
                        .requestMatchers("/api/submissions/**").permitAll()
                        .requestMatchers(HttpMethod.GET, "/api/rankings/**").permitAll()
                        
                        // Challenges
                        .requestMatchers(HttpMethod.GET, "/api/challenges", "/api/challenges/*").permitAll()
                        .requestMatchers("/api/challenges/**").authenticated()

                        // 퀴즈 생성/수정/삭제: 인증 필요
                        .requestMatchers(HttpMethod.POST, "/api/quizzes/**").authenticated()
                        .requestMatchers(HttpMethod.PUT, "/api/quizzes/**").authenticated()
                        .requestMatchers(HttpMethod.PATCH, "/api/quizzes/**").authenticated()
                        .requestMatchers(HttpMethod.DELETE, "/api/quizzes/**").authenticated()

                        // 5. 유저 정보 조회 ('/me' 우선 순위 주의)
                        .requestMatchers("/api/users/checkNickname").permitAll()
                        .requestMatchers(HttpMethod.GET, "/api/users/me", "/api/users/me/**").authenticated()

                        // 팝오버 조회 (게스트 허용)
                        .requestMatchers(HttpMethod.GET, "/api/users/*/popover").permitAll()

                        // 커스텀 아이템 조회 (전체 공개)
                        .requestMatchers(HttpMethod.GET, "/api/items/definitions").permitAll()
                        .requestMatchers(HttpMethod.GET, "/api/items/my").authenticated()

                        // 타인 퀴즈/프로필 조회 (게스트 허용)
                        .requestMatchers(HttpMethod.GET, "/api/users/*/quizzes").permitAll()
                        .requestMatchers(HttpMethod.GET, "/api/users/*").permitAll()

                        // 6. 기타 요청
                        .requestMatchers("/api/users/**").authenticated()
                        .requestMatchers("/api/follows/**").authenticated()

                        // [중요] 나머지 API 요청 인증 필수
                        .requestMatchers("/api/**").authenticated()

                        // [중요] 프론트엔드 라우트 허용 (WebController 위임)
                        .anyRequest().permitAll()
                )
                .httpBasic(basic -> basic.disable())
                .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }
}
