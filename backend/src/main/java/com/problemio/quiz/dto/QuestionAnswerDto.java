package com.problemio.quiz.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class QuestionAnswerDto {

    private Long id;
    @JsonProperty("text")
    private String answerText;
    private int sortOrder;
}
