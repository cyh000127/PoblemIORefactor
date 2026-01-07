package com.problemio.user.mapper;

import com.problemio.user.domain.RefreshToken;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.Optional;

@Mapper
public interface RefreshTokenMapper {

    // 토큰 저장 (기존에 해당 유저의 토큰이 있다면 덮어쓰거나, 삭제 후 insert)
    void save(RefreshToken refreshToken);

    // 토큰 값으로 조회 (유효성 검사용)
    Optional<RefreshToken> findByTokenValue(@Param("tokenValue") String tokenValue);

    // 유저 ID로 기존 토큰 삭제 (로그아웃 or 재로그인 시)
    void deleteByUserId(@Param("userId") Long userId);
}