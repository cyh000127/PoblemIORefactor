package com.problemio.user.dto;

import lombok.*;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.problemio.global.config.S3UrlSerializer;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UserPopoverResponse {

    private Long id;            // 대상 유저 ID
    private String nickname;        // 닉네임


    @JsonSerialize(using = S3UrlSerializer.class)
    private String profileImageUrl; // 프로필 이미지
    private String statusMessage;   // 상태 메시지
    private String profileTheme;
    private String avatarDecoration;
    private String popoverDecoration;

    private boolean isFollowing;    // 내가 이 유저를 팔로우 중인지
    private boolean isMe;           // 나 자신인지

    private int followerCount;      // 이 유저의 팔로워 수
    private int followingCount;     // 이 유저의 팔로잉 수
}
