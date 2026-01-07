package com.problemio.follow.dto;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class FollowUserDto {

    private Long id;
    private String nickname;
    private String profileImageUrl;
    private String statusMessage;
}
