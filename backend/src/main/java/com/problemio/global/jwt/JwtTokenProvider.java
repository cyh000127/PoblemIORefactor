package com.problemio.global.jwt;

import io.jsonwebtoken.*;

import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey; // Key 대신 SecretKey 사용 권장
import java.util.Date;

@Slf4j
@Component
public class JwtTokenProvider {

    private final SecretKey key;
    private final long accessTokenValidityInMilliseconds;

    public JwtTokenProvider(@Value("${jwt.secret}") String secretKey,
                            @Value("${jwt.expiration}") long validityInMilliseconds) {
        byte[] keyBytes = Decoders.BASE64.decode(secretKey);
        this.key = Keys.hmacShaKeyFor(keyBytes);
        this.accessTokenValidityInMilliseconds = validityInMilliseconds;
    }

    // Access Token 생성 (만료시간: 1시간)
    public String createAccessToken(String email) {
        return createToken(email, this.accessTokenValidityInMilliseconds);
    }

    // Refresh Token 생성 (만료시간: 2주 고정)
    public String createRefreshToken(String email) {
        
        long refreshTokenValidity = 14L * 24 * 60 * 60 * 1000; // 2주
        return createToken(email, refreshTokenValidity);
    }


    // 토큰 생성
    private String createToken(String email, long validityMs) {
        long now = (new Date()).getTime();
        Date validity = new Date(now + validityMs);

        return Jwts.builder()
                .subject(email)           // 토큰 제목 (유저 이메일)
                .issuedAt(new Date())     // 생성 시간
                .expiration(validity)     // 만료 시간
                .signWith(key)            // 암호화 서명
                .compact();
    }


    // 토큰에서 이메일(Subject) 추출
    public String getEmail(String token) {
        return parseClaims(token).getSubject();
    }

    // 토큰 유효성 검증
    public boolean validateToken(String token) {
        try {
            Jwts.parser()
                    .verifyWith(key)
                    .build()
                    .parseSignedClaims(token);
            return true;
        } catch (io.jsonwebtoken.security.SecurityException | MalformedJwtException e) {
            log.info("잘못된 JWT 서명입니다.");
        } catch (ExpiredJwtException e) {
            log.info("만료된 JWT 토큰입니다.");
        } catch (UnsupportedJwtException e) {
            log.info("지원되지 않는 JWT 토큰입니다.");
        } catch (IllegalArgumentException e) {
            log.info("JWT 토큰이 잘못되었습니다.");
        }
        return false;
    }

    private Claims parseClaims(String accessToken) {
        try {
            return Jwts.parser()
                    .verifyWith(key)
                    .build()
                    .parseSignedClaims(accessToken)
                    .getPayload();
        } catch (ExpiredJwtException e) {
            return e.getClaims();
        }
    }
}