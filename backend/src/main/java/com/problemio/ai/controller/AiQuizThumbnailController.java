package com.problemio.ai.controller;

import com.problemio.ai.dto.AiThumbnailCandidateRequest;
import com.problemio.ai.dto.AiThumbnailCandidateResponse;
import com.problemio.ai.dto.AiThumbnailConfirmRequest;
import com.problemio.ai.dto.AiThumbnailConfirmResponse;
import com.problemio.ai.service.AiQuizThumbnailService;
import com.problemio.global.auth.CustomUserDetails;
import com.problemio.global.common.ApiResponse;
import com.problemio.global.exception.BusinessException;
import com.problemio.global.exception.ErrorCode;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/ai/quiz-thumbnails")
@RequiredArgsConstructor
public class AiQuizThumbnailController {

    private final AiQuizThumbnailService aiQuizThumbnailService;

    @PostMapping("/candidates")
    public ResponseEntity<ApiResponse<AiThumbnailCandidateResponse>> createCandidates(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @Valid @RequestBody AiThumbnailCandidateRequest request
    ) {
        if (userDetails == null) {
            throw new BusinessException(ErrorCode.LOGIN_REQUIRED);
        }
        AiThumbnailCandidateResponse response = aiQuizThumbnailService.generateCandidates(
                request.getTitle(),
                request.getDescription()
        );
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @PostMapping("/confirm")
    public ResponseEntity<ApiResponse<AiThumbnailConfirmResponse>> confirmCandidate(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @Valid @RequestBody AiThumbnailConfirmRequest request
    ) {
        if (userDetails == null) {
            throw new BusinessException(ErrorCode.LOGIN_REQUIRED);
        }
        Long userId = userDetails.getUser().getId();
        AiThumbnailConfirmResponse response = aiQuizThumbnailService.confirmCandidate(
                request.getCandidateId(),
                userId
        );
        return ResponseEntity.ok(ApiResponse.success(response));
    }
}
