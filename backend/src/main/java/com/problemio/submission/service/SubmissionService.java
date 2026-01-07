package com.problemio.submission.service;

import com.problemio.submission.dto.QuizSubmissionRequest;
import com.problemio.submission.dto.QuizAnswerResponse;
import com.problemio.submission.dto.QuizSubmissionResponse;
import com.problemio.submission.dto.QuizPlayContextResponse;

public interface SubmissionService {

    QuizPlayContextResponse getPlayContext(Long quizId);

    QuizAnswerResponse submitQuiz(Long quizId, Long userIdOrNull, QuizSubmissionRequest request);

    QuizSubmissionResponse getSubmissionResult(Long submissionId);

    Long createSubmission(Long quizId, Long userId, Long challengeId);
}
