package com.problemio.user.usecase;


import com.problemio.global.exception.BusinessException;
import com.problemio.global.exception.ErrorCode;
import com.problemio.user.domain.DeleteStatus;
import com.problemio.user.domain.User;
import com.problemio.user.dto.UserResponse;
import com.problemio.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class GetUserProfileUseCase {

    private final UserRepository userRepository;

    public UserResponse execute(Long userId, Long viewerId) {
        // 1. 사용자 조회 (Entity)
        User user = userRepository.findByIdAndIsDeleted(userId, DeleteStatus.ACTIVE)
                .orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND));

        // 2. DTO 변환
        UserResponse response = UserResponse.from(user);

        // response.setPasswordHash(null); // 보안을 위해 비밀번호 해시 제거 (Setter 없음)
        
        // 3. 통계 정보 (JPA Count Query)
        int followerCount = userRepository.countFollwers(userId);
        int followingCount = userRepository.countFollowings(userId);
        
        response.setFollowingCount(followingCount);
        response.setFollowerCount(followerCount);
        // 4. 팔로우 여부 확인
        boolean isFollowedByMe = false;
        if (viewerId != null && viewerId > 0) {
            isFollowedByMe = userRepository.countByFollowerIdAndFollowingId(viewerId, userId) > 0;
        }
        response.setIsFollowedByMe(isFollowedByMe);
        
        // 5. 프로필 꾸미기 값 정규화
        response.setProfileTheme(normalizeDecorationValue(response.getProfileTheme()));
        response.setAvatarDecoration(normalizeDecorationValue(response.getAvatarDecoration()));
        response.setPopoverDecoration(normalizeDecorationValue(response.getPopoverDecoration()));

        return response;
    }

    private String normalizeDecorationValue(String value) {
        if (value == null || value.isBlank()) {
            return value;
        }
        String trimmed = value.trim();
        if (trimmed.startsWith("http://") || trimmed.startsWith("https://")) {
            String lastSegment = trimmed.substring(trimmed.lastIndexOf('/') + 1);
            int dotIdx = lastSegment.lastIndexOf('.');
            return (dotIdx > 0) ? lastSegment.substring(0, dotIdx) : lastSegment;
        }
        return trimmed;
    }
}
