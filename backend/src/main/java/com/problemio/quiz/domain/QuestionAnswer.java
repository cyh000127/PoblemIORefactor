package com.problemio.quiz.domain;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class QuestionAnswer {

    private Long id;
    private Long questionId;
    private String answerText;
    private int sortOrder;
}
