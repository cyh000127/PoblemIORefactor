package com.problemio.user.service;

import com.problemio.quiz.dto.QuizSummaryDto;
import com.problemio.user.dto.UserPopoverResponse;
import com.problemio.user.dto.UserResponse;
import com.problemio.user.dto.UserSummaryDto;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

public interface UserService {
    // 내 정보 조회
    UserResponse getUserById(Long id);

    // 상세 프로필 조회 (카운트, 팔로우 여부 포함)
    UserResponse getUserProfile(Long userId, Long viewerId);

    // 프로필 수정
    UserResponse updateProfile(Long userId, UserResponse request, MultipartFile file);

    // 비밀번호 변경
    void changePassword(Long userId, String oldPassword, String newPassword);

    // 회원 탈퇴
    void deleteAccount(Long userId, String password);

    // 마이페이지 요약
    UserSummaryDto getMySummary(Long userId);

    // 퀴즈 관련 (임시)
    List<QuizSummaryDto> getMyQuizzes(Long userId);
    List<QuizSummaryDto> getMyLikedQuizzes(Long userId);
    List<QuizSummaryDto> getFollowingQuizzes(Long userId);

    // 닉네임 중복 확인 (중복 시 예외 발생)
    void checkNicknameDuplicate(String nickname);

    // 팝오버용 프로필 조회
    UserPopoverResponse getUserPopover(Long userId, Long viewerId);
}
