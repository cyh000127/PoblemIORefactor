package com.problemio.follow.service;

import com.problemio.follow.dto.FollowUserDto;

import java.util.List;

public interface FollowService {

    void follow(Long followerId, Long targetUserId);

    void unfollow(Long followerId, Long targetUserId);

    boolean isFollowing(Long followerId, Long targetUserId);

    List<FollowUserDto> getFollowers(Long userId);

    List<FollowUserDto> getFollowings(Long userId);

    int getFollowerCount(Long userId);

    int getFollowingCount(Long userId);
}
