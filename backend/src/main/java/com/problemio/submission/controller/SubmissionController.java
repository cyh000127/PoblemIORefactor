package com.problemio.submission.controller;

import com.problemio.global.auth.CustomUserDetails;
import com.problemio.global.common.ApiResponse;
import com.problemio.submission.dto.QuizAnswerResponse;
import com.problemio.submission.dto.QuizPlayContextResponse;
import com.problemio.submission.dto.QuizSubmissionRequest;
import com.problemio.submission.dto.QuizSubmissionResponse;
import com.problemio.submission.service.SubmissionService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
public class SubmissionController {

    private final SubmissionService submissionService;

    @GetMapping("/api/quizzes/{quizId}/play-context")
    public ResponseEntity<ApiResponse<QuizPlayContextResponse>> getPlayContext(
            @PathVariable Long quizId) {
        return ResponseEntity.ok(
                ApiResponse.success(submissionService.getPlayContext(quizId))
        );
    }

    @PostMapping("/api/quizzes/{quizId}/submissions")
    public ResponseEntity<ApiResponse<QuizAnswerResponse>> submitQuiz(
            @PathVariable Long quizId,
            @RequestBody @Valid QuizSubmissionRequest request,
            @AuthenticationPrincipal CustomUserDetails userDetails) {

        Long userIdOrNull = userDetails != null ? userDetails.getUser().getId() : null;
        return ResponseEntity.ok(
                ApiResponse.success(submissionService.submitQuiz(quizId, userIdOrNull, request))
        );
    }

    @GetMapping("/api/submissions/{submissionId}")
    public ResponseEntity<ApiResponse<QuizSubmissionResponse>> getSubmissionResult(
            @PathVariable Long submissionId) {

        return ResponseEntity.ok(
                ApiResponse.success(submissionService.getSubmissionResult(submissionId))
        );
    }
}
