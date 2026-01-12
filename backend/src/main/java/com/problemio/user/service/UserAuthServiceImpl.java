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
import com.problemio.global.util.TimeUtils;
import com.problemio.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class UserAuthServiceImpl implements UserAuthService {

    private final UserRepository userRepository; // UserAuthMapper -> UserRepository 교체
    private final RefreshTokenMapper refreshTokenMapper;
    private final PasswordEncoder passwordEncoder;
    private final JwtTokenProvider jwtTokenProvider;
    private final EmailService emailService;

    @Override
    @Transactional
    public UserResponse signup(UserSignupRequest request) {
        if (userRepository.existsByEmail(request.getEmail())) {
            throw new BusinessException(ErrorCode.EMAIL_DUPLICATED);
        }
        if (userRepository.existsByNickname(request.getNickname())) {
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

        User user = User.builder()
                .email(request.getEmail())
                .password(passwordEncoder.encode(request.getPassword()))
                .nickname(request.getNickname())
                .build();

        userRepository.save(user);
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
        // JPA Repository 사용 (isActive 체크는 User.getStatus()로)
        User user = userRepository.findByEmail(request.getEmail())
                .orElseThrow(() -> new BusinessException(ErrorCode.INVALID_LOGIN));

        if (!user.getStatus()) {
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
        User user = userRepository.findByEmail(email)
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

        User user = userRepository.findById(dbToken.getUserId())
                .orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND));

        if (!user.getStatus()) {
            throw new BusinessException(ErrorCode.USER_NOT_FOUND);
        }

        String newAccessToken = jwtTokenProvider.createAccessToken(user.getEmail());
        return new TokenResponse(newAccessToken, refreshToken);
    }
}
