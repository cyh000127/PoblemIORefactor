package com.problemio.submission.domain;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class SubmissionDetail {

    private Long id;
    private Long submissionId;
    private Long questionId;
    private String userAnswer;
    private boolean isCorrect;
}
