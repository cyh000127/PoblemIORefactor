package com.problemio.user.service;

import com.problemio.user.dto.TokenResponse;
import com.problemio.user.dto.UserLoginRequest;
import com.problemio.user.dto.UserResponse;
import com.problemio.user.dto.UserSignupRequest;

public interface UserAuthService {
    // 회원가입
    UserResponse signup(UserSignupRequest request);

    // 로그인
    TokenResponse login(UserLoginRequest request);

    // 로그아웃
    void logout(String email);

    // 토큰 재발급
    TokenResponse reissue(String refreshToken);
}