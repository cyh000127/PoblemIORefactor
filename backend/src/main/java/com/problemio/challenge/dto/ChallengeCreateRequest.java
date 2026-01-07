package com.problemio.challenge.dto;

import com.problemio.challenge.domain.Challenge;
import lombok.Data;

import jakarta.validation.constraints.Future;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.time.LocalDateTime;

@Data
public class ChallengeCreateRequest {

    @NotBlank(message = "챌린지 제목은 필수입니다.")
    private String title;

    private String description;

    @NotBlank(message = "챌린지 유형은 필수입니다.")
    private String challengeType; // TIME_ATTACK, SURVIVAL

    @NotNull(message = "대상 퀴즈 ID는 필수입니다.")
    private Long targetQuizId;

    private Integer timeLimit; // 초 단위

    @NotNull(message = "시작일은 필수입니다.")
    private LocalDateTime startAt;

    @NotNull(message = "종료일은 필수입니다.")
    @Future(message = "종료일은 미래여야 합니다.")
    private LocalDateTime endAt;

    public Challenge toEntity() {
        Challenge challenge = new Challenge();
        challenge.setTitle(this.title);
        challenge.setDescription(this.description);
        challenge.setChallengeType(this.challengeType);
        challenge.setTargetQuizId(this.targetQuizId);
        challenge.setTimeLimit(this.timeLimit != null ? this.timeLimit : 0);
        challenge.setStartAt(this.startAt);
        challenge.setEndAt(this.endAt);
        return challenge;
    }
}
