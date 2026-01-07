package com.problemio.user.dto;

import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.problemio.global.config.S3UrlSerializer;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserResponse {
    private Long id;
    private String email; // username
    private String nickname;
    
    @JsonSerialize(using = S3UrlSerializer.class)
    private String profileImageUrl;
    private String profileTheme;
    private String avatarDecoration;
    private String popoverDecoration;
    private String statusMessage;
    private Boolean isDeleted;

    private int followerCount;
    private int followingCount;
    private int quizCount;
    private Boolean isFollowedByMe;
    private String role;
    private java.time.LocalDateTime updatedAt;
}
