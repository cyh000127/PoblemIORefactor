package com.problemio.quiz.domain;

import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter
@Setter
public class QuizLike {

    private Long userId;
    private Long quizId;
    private LocalDateTime createdAt;
}
