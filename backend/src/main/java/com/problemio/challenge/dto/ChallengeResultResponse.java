package com.problemio.challenge.dto;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class ChallengeResultResponse {
    private Long challengeId;
    private Long submissionId;
    private int rank;           // 등수
    private int totalParticipants; // 전체 참여자 수 (옵션)
    
    // Survival
    private Integer correctCount;
    
    // Time Attack
    private Double playTime;   // 초 단위 (또는 ms)
    private String formattedTime; // "00:00.000"
    private String challengeType; // TIME_ATTACK or SURVIVAL
}
