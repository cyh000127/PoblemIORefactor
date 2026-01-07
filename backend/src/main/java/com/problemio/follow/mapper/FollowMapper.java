// src/main/java/com/problemio/follow/mapper/FollowMapper.java
package com.problemio.follow.mapper;

import com.problemio.follow.domain.Follow;
import com.problemio.follow.dto.FollowUserDto;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface FollowMapper {

    int insert(Follow follow);

    int delete(@Param("followerId") Long followerId,
               @Param("followingId") Long followingId);

    int exists(@Param("followerId") Long followerId,
               @Param("followingId") Long followingId);

    List<FollowUserDto> findFollowers(@Param("userId") Long userId);

    List<FollowUserDto> findFollowings(@Param("userId") Long userId);

    int countFollowers(@Param("userId") Long userId);

    int countFollowings(@Param("userId") Long userId);

    int deleteByUserId(@Param("userId") Long userId);
}
