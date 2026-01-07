package com.problemio.challenge.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.problemio.global.config.S3UrlSerializer;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ChallengeRankingResponse {
    private Long challengeId;
    private Long userId;
    private String nickname;


    @JsonSerialize(using = S3UrlSerializer.class)
    private String profileImageUrl;
    private Integer ranking;
    private Double score;
    private Double playTime;
    private String challengeType; // TIME_ATTACK or SURVIVAL
    private LocalDateTime recordedAt;
}
