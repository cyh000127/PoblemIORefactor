package com.problemio.auth.service;

import com.problemio.auth.dto.LoginRequest;
import com.problemio.auth.dto.SignupRequest;
import com.problemio.auth.dto.TokenResponse;
import com.problemio.user.dto.UserResponse;

public interface AuthService {
    UserResponse signup(SignupRequest request);
    TokenResponse login(LoginRequest request);
    void logout(String email);
    TokenResponse reissue(String refreshToken);
}
