package com.problemio.quiz.domain;

import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter
@Setter
public class Question {

    private Long id;
    private Long quizId;
    private int questionOrder;
    private String imageUrl;
    private String description;
    private LocalDateTime createdAt;
}
