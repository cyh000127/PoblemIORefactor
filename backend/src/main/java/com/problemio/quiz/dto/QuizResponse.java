package com.problemio.quiz.dto;

import com.problemio.quiz.dto.QuestionResponse;
import com.problemio.user.dto.UserResponse;
import lombok.Builder;
import lombok.Getter;

import java.util.List;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.problemio.global.config.S3UrlSerializer;

@Getter
@Builder
public class QuizResponse {

    private Long id;
    private Long userId;
    private String title;
    private String description;


    @JsonSerialize(using = S3UrlSerializer.class)
    private String thumbnailUrl;
    private boolean isPublic;
    private int likeCount;
    private int playCount;
    private List<QuestionResponse> questions;
    private UserResponse author;
    private Boolean isLikedByMe;
    private Boolean isFollowedByMe;
    private boolean isHidden;
}
