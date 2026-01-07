package com.problemio.challenge.dto;

import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
@Builder
public class ChallengeDto {
    private Long id;
    private String title;
    private String description;
    private String challengeType; // TIME_ATTACK, SURVIVAL
    private Integer timeLimit;
    private LocalDateTime startAt;
    private LocalDateTime endAt;
    private com.problemio.quiz.dto.QuizResponse targetQuiz;
}
