package com.problemio.quiz.dto;

import com.problemio.quiz.dto.QuestionCreateRequest;
import lombok.Getter;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
public class QuizUpdateRequest {

    private String title;
    private String description;
    private String thumbnailUrl;
    private Boolean isPublic;
    private List<QuestionCreateRequest> questions;
}
