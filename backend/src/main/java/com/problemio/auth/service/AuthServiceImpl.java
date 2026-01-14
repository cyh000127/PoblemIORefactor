package com.problemio.auth.service;

import com.problemio.auth.domain.RefreshToken;
import com.problemio.auth.dto.LoginRequest;
import com.problemio.auth.dto.SignupRequest;
import com.problemio.auth.dto.TokenResponse;
import com.problemio.auth.repository.RefreshTokenRepository;
import com.problemio.global.exception.BusinessException;
import com.problemio.global.exception.ErrorCode;
import com.problemio.global.jwt.JwtTokenProvider;
import com.problemio.global.util.TimeUtils;
import com.problemio.user.domain.User;
import com.problemio.user.dto.UserResponse;
import com.problemio.user.repository.UserRepository;
import com.problemio.user.service.EmailService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class AuthServiceImpl implements AuthService {

    private final UserRepository userRepository;
    private final RefreshTokenRepository refreshTokenRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtTokenProvider jwtTokenProvider;
    private final EmailService emailService;

    @Override
    @Transactional
    public UserResponse signup(SignupRequest request) {
        if (userRepository.existsByEmail(request.getEmail())) {
            throw new BusinessException(ErrorCode.EMAIL_DUPLICATED);
        }
        if (userRepository.existsByNickname(request.getNickname())) {
            throw new BusinessException(ErrorCode.NICKNAME_DUPLICATED);
        }

        // 금칙어 검사
        String nickname = request.getNickname();
        if (nickname.contains("admin") || nickname.contains("관리자") || nickname.contains("운영자")) {
             throw new BusinessException(ErrorCode.INVALID_INPUT_VALUE);
        }

        User user = User.builder()
                .email(request.getEmail())
                .password(passwordEncoder.encode(request.getPassword()))
                .nickname(request.getNickname())
                .build();

        userRepository.save(user);

        return UserResponse.builder()
                .id(user.getId())
                .email(user.getEmail())
                .nickname(user.getNickname())
                .build();
    }

    @Override
    @Transactional
    public TokenResponse login(LoginRequest request) {
        User user = userRepository.findByEmail(request.getEmail())
                .orElseThrow(() -> new BusinessException(ErrorCode.INVALID_LOGIN));

        if (!user.getStatus()) {
            throw new BusinessException(ErrorCode.USER_NOT_FOUND);
        }

        if (!passwordEncoder.matches(request.getPassword(), user.getPasswordHash())) {
            throw new BusinessException(ErrorCode.INVALID_LOGIN);
        }

        String accessToken = jwtTokenProvider.createAccessToken(user.getEmail());
        String refreshTokenVal = jwtTokenProvider.createRefreshToken(user.getEmail());

        // 파생된 삭제 쿼리를 사용하여 기존 토큰 삭제 (User 엔티티 참조)
        // 인터페이스에 @Query 없이 User를 직접 받는 deleteByUser 메서드가 없으므로,
        // 리포지토리에 정의된 deleteByUser_Id(Long userId)를 사용합니다.
        refreshTokenRepository.deleteByUser_Id(user.getId());

        // 새로운 토큰 저장
        refreshTokenRepository.save(RefreshToken.builder()
                .user(user)
                .tokenValue(refreshTokenVal)
                .expiresAt(TimeUtils.now().plusWeeks(2))
                .build());

        return new TokenResponse(accessToken, refreshTokenVal);
    }

    @Override
    @Transactional
    public void logout(String email) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND));
        refreshTokenRepository.deleteByUser_Id(user.getId());
    }

    @Override
    @Transactional
    public TokenResponse reissue(String refreshToken) {
        if (refreshToken == null || refreshToken.isBlank()) {
            throw new BusinessException(ErrorCode.ACCESS_DENIED);
        }
        if (!jwtTokenProvider.validateToken(refreshToken)) {
            throw new BusinessException(ErrorCode.ACCESS_DENIED);
        }
        RefreshToken dbToken = refreshTokenRepository.findByTokenValue(refreshToken)
                .orElseThrow(() -> new BusinessException(ErrorCode.ACCESS_DENIED));

        User user = dbToken.getUser();

        if (!user.getStatus()) {
            throw new BusinessException(ErrorCode.USER_NOT_FOUND);
        }

        String newAccessToken = jwtTokenProvider.createAccessToken(user.getEmail());
        return new TokenResponse(newAccessToken, refreshToken);
    }
}
