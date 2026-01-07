package com.problemio.user.controller;

import com.problemio.global.common.ApiResponse;
import com.problemio.user.dto.TokenResponse;
import com.problemio.user.dto.UserLoginRequest;
import com.problemio.user.dto.UserResponse;
import com.problemio.user.dto.UserSignupRequest;
import com.problemio.user.service.UserAuthService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseCookie;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class UserAuthController {

    private final UserAuthService userAuthService;


    //회원 가입
    @PostMapping("/signup")
    public ResponseEntity<ApiResponse<UserResponse>> signup(@RequestBody @Valid UserSignupRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success(userAuthService.signup(request)));
    }

    // 로그인
    @PostMapping("/login")
    public ResponseEntity<ApiResponse<TokenResponse>> login(@RequestBody @Valid UserLoginRequest request) {
        TokenResponse tokens = userAuthService.login(request);

        ResponseCookie refreshCookie = ResponseCookie.from("refreshToken", tokens.getRefreshToken())
                .httpOnly(true)
                .secure(true) // HTTPS 배포 환경이므로 true로 설정
                .sameSite("Lax")
                .path("/api/auth")
                .maxAge(14 * 24 * 60 * 60) // 14일
                .build();

        // accessToken만 body로 사용
        TokenResponse body = TokenResponse.builder()
                .accessToken(tokens.getAccessToken())
                .build();

        return ResponseEntity.ok()
                .header("Set-Cookie", refreshCookie.toString())
                .body(ApiResponse.success(body));
    }


    // 로그아웃
    @PostMapping("/logout")
    public ResponseEntity<ApiResponse<Void>> logout(@AuthenticationPrincipal UserDetails userDetails) {
        if (userDetails != null) {
            userAuthService.logout(userDetails.getUsername());
        }

        ResponseCookie clearCookie = ResponseCookie.from("refreshToken", "")
                .httpOnly(true)
                .secure(true) // HTTPS 배포 환경이므로 true로 설정
                .sameSite("Lax")
                .path("/api/auth")
                .maxAge(0)
                .build();

        return ResponseEntity.ok()
                .header("Set-Cookie", clearCookie.toString())
                .body(ApiResponse.success(null));
    }

    // 토큰 재발급
    @PostMapping("/reissue")
    public ResponseEntity<ApiResponse<TokenResponse>> reissue(@CookieValue(value = "refreshToken", required = false) String refreshToken) {
        System.out.println("DEBUG: Reissue requested. RefreshToken: " + (refreshToken == null ? "NULL" : refreshToken.substring(0, Math.min(10, refreshToken.length())) + "..."));
        TokenResponse tokens = userAuthService.reissue(refreshToken);

        // refreshToken 재설정 (만료 임박 대비)
        ResponseCookie refreshCookie = ResponseCookie.from("refreshToken", tokens.getRefreshToken())
                .httpOnly(true)
                .secure(true) // HTTPS 배포 환경이므로 true로 설정
                .sameSite("Lax")
                .path("/api/auth")
                .maxAge(14 * 24 * 60 * 60)
                .build();

        TokenResponse body = TokenResponse.builder()
                .accessToken(tokens.getAccessToken())
                .build();

        return ResponseEntity.ok()
                .header("Set-Cookie", refreshCookie.toString())
                .body(ApiResponse.success(body));
    }
}
