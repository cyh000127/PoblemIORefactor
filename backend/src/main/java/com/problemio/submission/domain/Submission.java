package com.problemio.submission.domain;

import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter
@Setter
public class Submission {

    private Long id;
    private Long quizId;
    private Long userId; // 비회원은 null
    private int totalQuestions;
    private int correctCount;
    private LocalDateTime submittedAt;
    private Long challengeId;
    private Double playTime;
}
