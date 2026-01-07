package com.problemio.user.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserSummaryDto {
    private Long userId;
    private String nickname;
    private int followerCount;
    private int followingCount;
    private int createdQuizCount; // 만든 퀴즈 수
}