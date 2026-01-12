package com.problemio.user.service;

import com.problemio.quiz.dto.QuizSummaryDto;
import com.problemio.user.dto.UserPopoverResponse;
import com.problemio.user.dto.UserResponse;
import com.problemio.user.dto.UserSummaryDto;
import com.problemio.user.usecase.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class UserServiceImpl implements UserService {

    private final GetUserUseCase getUserUseCase;
    private final GetUserProfileUseCase getUserProfileUseCase;
    private final UpdateProfileUseCase updateProfileUseCase;
    private final ChangePasswordUseCase changePasswordUseCase;
    private final DeleteAccountUseCase deleteAccountUseCase;
    private final CheckNicknameUseCase checkNicknameUseCase;
    private final GetUserPopoverUseCase getUserPopoverUseCase;
    private final GetUserSummaryUseCase getUserSummaryUseCase;

    @Override
    public UserResponse getUserById(Long id) {
        return getUserUseCase.execute(id);
    }

    @Override
    public UserResponse getUserProfile(Long userId, Long viewerId) {
        return getUserProfileUseCase.execute(userId, viewerId);
    }

    @Override
    public UserResponse updateProfile(Long userId, UserResponse request, MultipartFile file) {
        return updateProfileUseCase.execute(userId, request, file);
    }

    @Override
    public void changePassword(Long userId, String oldPassword, String newPassword) {
        changePasswordUseCase.execute(userId, oldPassword, newPassword);
    }

    @Override
    public void deleteAccount(Long userId, String password) {
        deleteAccountUseCase.execute(userId, password);
    }

    @Override
    public UserSummaryDto getMySummary(Long userId) {
        return getUserSummaryUseCase.execute(userId);
    }

    @Override
    public void checkNicknameDuplicate(String nickname) {
        checkNicknameUseCase.execute(nickname);
    }

    @Override
    public UserPopoverResponse getUserPopover(Long userId, Long viewerId) {
        return getUserPopoverUseCase.execute(userId, viewerId);
    }

    // == 임시/미구현 메서드들 (기존 유지) ==

    @Override
    public List<QuizSummaryDto> getMyQuizzes(Long userId) {
        // 추후 구현 필요
        return List.of();
    }

    @Override
    public List<QuizSummaryDto> getMyLikedQuizzes(Long userId) {
        // 추후 구현 필요
        return List.of();
    }

    @Override
    public List<QuizSummaryDto> getFollowingQuizzes(Long userId) {
        // 추후 구현 필요
        return List.of();
    }
}
