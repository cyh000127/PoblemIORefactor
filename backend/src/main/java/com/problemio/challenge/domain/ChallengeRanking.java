package com.problemio.challenge.domain;

import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter
@Setter
public class ChallengeRanking {

    private Long id;
    private Long challengeId;
    private Long userId;
    private Integer ranking;
    private Double score;
    private Double playTime;
    private LocalDateTime createdAt;
}
