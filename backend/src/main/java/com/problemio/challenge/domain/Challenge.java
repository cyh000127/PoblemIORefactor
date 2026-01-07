package com.problemio.challenge.domain;

import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter
@Setter
public class Challenge {

    private Long id;
    private String title;
    private String description;
    private String challengeType; // TIME_ATTACK, SURVIVAL
    private Long targetQuizId;
    private Integer timeLimit;
    private LocalDateTime startAt;
    private LocalDateTime endAt;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
