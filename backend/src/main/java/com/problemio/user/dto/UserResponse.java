package com.problemio.user.dto;

import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.problemio.global.config.FileUrlSerializer;
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
    
    @JsonSerialize(using = FileUrlSerializer.class)
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

    public static UserResponse from(com.problemio.user.domain.User user) {
        return UserResponse.builder()
                .id(user.getId())
                .email(user.getEmail())
                .nickname(user.getNickname())
                .profileImageUrl(user.getProfileImageUrl())
                .statusMessage(user.getStatusMessage())
                .isDeleted(!user.getStatus())
                .role(user.getRole().name())
                .updatedAt(user.getUpdatedAt())
                .build();
    }
}
