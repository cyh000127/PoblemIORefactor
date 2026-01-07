package com.problemio.challenge.dto;

import com.problemio.quiz.dto.QuestionResponse;
import lombok.Builder;
import lombok.Getter;

import java.util.List;

@Getter
@Builder
public class ChallengeStartResponse {
    private Long challengeId;
    private Long submissionId;
    private String challengeType;
    private Integer timeLimit;
    private List<QuestionResponse> questions;
}
