// src/main/java/com/problemio/follow/controller/FollowController.java
package com.problemio.follow.controller;

import com.problemio.follow.dto.FollowUserDto;
import com.problemio.follow.service.FollowService;
import com.problemio.global.common.ApiResponse;
import com.problemio.global.auth.CustomUserDetails;
import com.problemio.global.exception.BusinessException;
import com.problemio.global.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/follows")
public class FollowController {

    private final FollowService followService;

    // 팔로우 요청
    @PostMapping("/{targetUserId}")
    public ApiResponse<Void> follow(@AuthenticationPrincipal CustomUserDetails userDetails,
                                    @PathVariable Long targetUserId) {
        Long loginUserId = requireLogin(userDetails);
        followService.follow(loginUserId, targetUserId);
        return ApiResponse.success(null);
    }

    // 언팔로우 요청
    @DeleteMapping("/{targetUserId}")
    public ApiResponse<Void> unfollow(@AuthenticationPrincipal CustomUserDetails userDetails,
                                      @PathVariable Long targetUserId) {
        Long loginUserId = requireLogin(userDetails);
        followService.unfollow(loginUserId, targetUserId);
        return ApiResponse.success(null);
    }

    // 팔로우 여부 확인
    @GetMapping("/{targetUserId}/status")
    public ApiResponse<Boolean> isFollowing(@AuthenticationPrincipal CustomUserDetails userDetails,
                                            @PathVariable Long targetUserId) {
        Long loginUserId = requireLogin(userDetails);
        boolean result = followService.isFollowing(loginUserId, targetUserId);
        return ApiResponse.success(result);
    }

    // 팔로워 목록 조회
    @GetMapping("/{userId}/followers")
    public ApiResponse<List<FollowUserDto>> getFollowers(@PathVariable Long userId) {
        return ApiResponse.success(followService.getFollowers(userId));
    }

    // 팔로잉 목록 조회
    @GetMapping("/{userId}/followings")
    public ApiResponse<List<FollowUserDto>> getFollowings(@PathVariable Long userId) {
        return ApiResponse.success(followService.getFollowings(userId));
    }

    private Long requireLogin(CustomUserDetails userDetails) {
        if (userDetails == null) {
            throw new BusinessException(ErrorCode.LOGIN_REQUIRED);
        }
        return userDetails.getUser().getId();
    }
}
