package com.problemio.quiz.dto;

import lombok.*;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.problemio.global.config.S3UrlSerializer;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class QuizSummaryDto {

    private Long id;
    private String title;
    private String description;


    @JsonSerialize(using = S3UrlSerializer.class)
    private String thumbnailUrl;


    private int likeCount;
    private int playCount;
    private Integer commentCount;
    private boolean hidden;

}
