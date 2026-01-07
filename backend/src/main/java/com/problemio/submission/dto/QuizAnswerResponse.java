package com.problemio.submission.dto;

import lombok.Builder;
import lombok.Getter;

import java.util.List;

@Getter
@Builder
public class QuizAnswerResponse {
    private Long submissionId;
    private Long questionId;
    private boolean correct;
    private String correctAnswer;
    private List<String> correctAnswers;
    private String imageUrl;
    private int totalQuestions;
    private int answeredCount;
    private int correctCount;
}
