package com.problemio.quiz.dto;

import lombok.Builder;
import lombok.Getter;

import java.util.List;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.problemio.global.config.FileUrlSerializer;

@Getter
@Builder
public class QuestionResponse {
    private Long id;
    private int order;
    private String description;


    @JsonSerialize(using = FileUrlSerializer.class)
    private String imageUrl;
    private List<QuestionAnswerDto> answers;
}
