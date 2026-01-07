package com.problemio.quiz.dto;

import jakarta.validation.constraints.NotBlank;
import com.fasterxml.jackson.annotation.JsonAlias;
import lombok.Getter;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
public class QuestionCreateRequest {

    @JsonAlias("order")
    private Integer questionOrder;

    @NotBlank
    private String imageUrl;

    private String description;

    private List<AnswerCreateRequest> answers;
}
