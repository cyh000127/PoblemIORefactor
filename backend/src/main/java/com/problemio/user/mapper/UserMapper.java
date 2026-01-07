package com.problemio.user.mapper;

import com.problemio.user.dto.UserPopoverResponse;
import com.problemio.user.dto.UserResponse;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Mapper
public interface UserMapper {

    // 1. 마이페이지/상대방 프로필 조회 (비밀번호 제외된 DTO)
    Optional<UserResponse> findById(Long id);

    List<UserResponse> findByIds(@Param("ids") List<Long> ids);

    // 2. 프로필 수정 (닉네임, 이미지, 상태메시지)
    void updateProfile(UserResponse user);

    // 3. 비밀번호 변경
    void updatePassword(@Param("id") Long id, @Param("passwordHash") String passwordHash, @Param("updatedAt") LocalDateTime updatedAt);

    // 4. 회원 탈퇴 (Soft Delete)
    void deleteUser(@Param("id") Long id, @Param("updatedAt") LocalDateTime updatedAt);

    // 5. 마이페이지 통계용 카운트
    int countFollowers(Long userId);
    int countFollowings(Long userId);
    int countCreatedQuizzes(Long userId); // 퀴즈 DTO는 없어도 개수는 셀 수 있음

   // 6. 닉네임 중복 존재 여부 확인 (0: 없음, 1이상: 존재)
    int existsByNickname(String nickname);

    // 7. 팝오버 요약 정보
    UserPopoverResponse findUserPopover(
            @Param("userId") Long userId,
            @Param("viewerId") Long viewerId
    );

    void anonymizeCredentials(@Param("id") Long id,
                              @Param("email") String email,
                              @Param("nickname") String nickname,
                              @Param("updatedAt") LocalDateTime updatedAt);

    // 8. 프로필 상세 조회 (카운트 포함)
    UserResponse findUserProfile(@Param("userId") Long userId, @Param("viewerId") Long viewerId);
}
