package com.problemio.quiz.dto;

import lombok.Builder;
import lombok.Getter;

import java.util.List;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.problemio.global.config.S3UrlSerializer;

@Getter
@Builder
public class QuestionResponse {
    private Long id;
    private int order;
    private String description;


    @JsonSerialize(using = S3UrlSerializer.class)
    private String imageUrl;
    private List<QuestionAnswerDto> answers;
}
