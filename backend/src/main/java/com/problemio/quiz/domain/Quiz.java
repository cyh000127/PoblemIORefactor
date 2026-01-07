package com.problemio.quiz.domain;

import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter
@Setter
public class Quiz {

    private Long id;
    private Long userId;
    private String title;
    private String description;
    private String thumbnailUrl;
    private boolean isPublic;
    private int likeCount;
    private int playCount;
    private boolean isHidden;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
