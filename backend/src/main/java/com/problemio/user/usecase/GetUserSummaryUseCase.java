package com.problemio.user.usecase;

import com.problemio.global.exception.BusinessException;
import com.problemio.global.exception.ErrorCode;
import com.problemio.user.domain.DeleteStatus;
import com.problemio.user.domain.User;
import com.problemio.user.dto.UserSummaryDto;
import com.problemio.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class GetUserSummaryUseCase {

    private final UserRepository userRepository;

    public UserSummaryDto execute(Long userId) {
        User user = userRepository.findByIdAndIsDeleted(userId, DeleteStatus.ACTIVE)
                .orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND));

        int followerCount = userRepository.countFollowers(userId);
        int followingCount = userRepository.countFollowings(userId);
        long createdQuizCount = userRepository.countCreatedQuizzes(userId);

        return UserSummaryDto.builder()
                .userId(userId)
                .nickname(user.getNickname())
                .followerCount(followerCount)
                .followingCount(followingCount)
                .createdQuizCount((int) createdQuizCount)
                .build();
    }
}
