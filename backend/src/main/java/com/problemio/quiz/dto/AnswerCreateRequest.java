package com.problemio.quiz.dto;

import com.fasterxml.jackson.annotation.JsonAlias;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class AnswerCreateRequest {
    @JsonAlias("text")
    @JsonProperty("text")
    private String answerText; //

    private Integer sortOrder;
}
