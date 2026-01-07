package com.problemio.submission.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class QuizPlayContextResponse {
    private Long quizId;
    private String title;
    private String description;
    private String thumbnailUrl;
    private int totalQuestions;
    private List<QuestionDto> questions;

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class QuestionDto {
        private Long id;
        private Integer order;
        private String imageUrl;
        private String description;
    }
}
