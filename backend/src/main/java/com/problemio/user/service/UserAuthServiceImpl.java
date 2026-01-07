package com.problemio.user.service;

import com.problemio.global.exception.BusinessException;
import com.problemio.global.exception.ErrorCode;
import com.problemio.global.jwt.JwtTokenProvider;
import com.problemio.user.domain.RefreshToken;
import com.problemio.user.domain.User;
import com.problemio.user.dto.TokenResponse;
import com.problemio.user.dto.UserLoginRequest;
import com.problemio.user.dto.UserResponse;
import com.problemio.user.dto.UserSignupRequest;
import com.problemio.user.mapper.RefreshTokenMapper;
import com.problemio.user.mapper.UserAuthMapper;
import com.problemio.global.util.TimeUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class UserAuthServiceImpl implements UserAuthService {

    private final UserAuthMapper userAuthMapper;
    private final RefreshTokenMapper refreshTokenMapper;
    private final PasswordEncoder passwordEncoder;
    private final JwtTokenProvider jwtTokenProvider;
    private final EmailService emailService;

    @Override
    @Transactional
    public UserResponse signup(UserSignupRequest request) {
        if (userAuthMapper.findByEmail(request.getEmail()).isPresent()) {
            throw new BusinessException(ErrorCode.EMAIL_DUPLICATED);
        }
        if (userAuthMapper.findByNickname(request.getNickname()).isPresent()) {
            throw new BusinessException(ErrorCode.NICKNAME_DUPLICATED);
        }

        // 이메일 인증 여부 확인 (백엔드 강제) - 로컬 환경을 위해 주석 처리
        // if (!emailService.isEmailVerified(request.getEmail())) {
        //     throw new BusinessException(ErrorCode.EMAIL_NOT_VERIFIED);
        // }

        // 금칙어 검사
        String nickname = request.getNickname();
        if (nickname.contains("admin") || nickname.contains("관리자") || nickname.contains("운영자")) {
             throw new BusinessException(ErrorCode.INVALID_INPUT_VALUE); // 적절한 에러 코드로 대체 필요할 수 있음
        }

        User user = new User();
        user.setEmail(request.getEmail());
        user.setNickname(request.getNickname());
        user.setPasswordHash(passwordEncoder.encode(request.getPassword()));

        userAuthMapper.insertUser(user);

        // 인증 정보 사용 처리 (재사용 방지) - 로컬 환경을 위해 주석 처리
        // emailService.consumeVerification(request.getEmail());

        return UserResponse.builder()
                .id(user.getId())
                .email(user.getEmail())
                .nickname(user.getNickname())
                .build();
    }

    @Override
    @Transactional
    public TokenResponse login(UserLoginRequest request) {
        User user = userAuthMapper.findByEmail(request.getEmail())
                .orElseThrow(() -> new BusinessException(ErrorCode.INVALID_LOGIN));

        if (user.isDeleted()) {
            throw new BusinessException(ErrorCode.USER_NOT_FOUND);
        }

        if (!passwordEncoder.matches(request.getPassword(), user.getPasswordHash())) {
            throw new BusinessException(ErrorCode.INVALID_LOGIN);
        }

        String accessToken = jwtTokenProvider.createAccessToken(user.getEmail());
        String refreshToken = jwtTokenProvider.createRefreshToken(user.getEmail());

        refreshTokenMapper.deleteByUserId(user.getId());
        refreshTokenMapper.save(RefreshToken.builder()
                .userId(user.getId())
                .tokenValue(refreshToken)
                .expiresAt(TimeUtils.now().plusWeeks(2))
                .build());

        return new TokenResponse(accessToken, refreshToken);
    }

    @Override
    @Transactional
    public void logout(String email) {
        User user = userAuthMapper.findByEmail(email)
                .orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND));
        refreshTokenMapper.deleteByUserId(user.getId());
    }

    @Override
    @Transactional
    public TokenResponse reissue(String refreshToken) {
        if (refreshToken == null || refreshToken.isBlank()) {
            System.out.println("DEBUG: Reissue failed - Token is null/blank");
            throw new BusinessException(ErrorCode.ACCESS_DENIED);
        }
        if (!jwtTokenProvider.validateToken(refreshToken)) {
            System.out.println("DEBUG: Reissue failed - Token validation failed");
            throw new BusinessException(ErrorCode.ACCESS_DENIED);
        }
        RefreshToken dbToken = refreshTokenMapper.findByTokenValue(refreshToken)
                .orElseThrow(() -> new BusinessException(ErrorCode.ACCESS_DENIED));

        User user = userAuthMapper.findById(dbToken.getUserId())
                .orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND));

        if (user.isDeleted()) {
            throw new BusinessException(ErrorCode.USER_NOT_FOUND);
        }

        String newAccessToken = jwtTokenProvider.createAccessToken(user.getEmail());
        return new TokenResponse(newAccessToken, refreshToken);
    }
}
