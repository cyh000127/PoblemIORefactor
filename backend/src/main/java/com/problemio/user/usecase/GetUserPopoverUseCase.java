package com.problemio.user.usecase;

import com.problemio.global.exception.BusinessException;
import com.problemio.global.exception.ErrorCode;
import com.problemio.user.domain.DeleteStatus;
import com.problemio.user.domain.User;
import com.problemio.user.dto.UserPopoverResponse;
import com.problemio.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class GetUserPopoverUseCase {

    private final UserRepository userRepository;

    public UserPopoverResponse execute(Long userId, Long viewerId) {
        User user = userRepository.findByIdAndIsDeleted(userId, DeleteStatus.ACTIVE)
                .orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND));

        int followerCount = userRepository.countFollowers(userId);
        int followingCount = userRepository.countFollowings(userId);

        boolean isFollowing = false;
        boolean isMe = false;
        if (viewerId != null) {
            isMe = userId.equals(viewerId);
            if (!isMe) {
                isFollowing = userRepository.countByFollowerIdAndFollowingId(viewerId, userId) > 0;
            }
        }

        return UserPopoverResponse.builder()
                .id(user.getId())
                .nickname(user.getNickname())
                .profileImageUrl(user.getProfileImageUrl())
                .statusMessage(user.getStatusMessage())
                .profileTheme(normalizeDecorationValue(user.getProfileTheme()))
                .avatarDecoration(normalizeDecorationValue(user.getAvatarDecoration()))
                .popoverDecoration(normalizeDecorationValue(user.getPopoverDecoration()))
                .isFollowing(isFollowing)
                .isMe(isMe)
                .followerCount(followerCount)
                .followingCount(followingCount)
                .build();
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
