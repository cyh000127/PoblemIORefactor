package com.problemio.submission.dto;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class QuizSubmissionResponse {

    private Long submissionId;
    private int totalQuestions;
    private int correctCount;
    private int answeredCount;
}
