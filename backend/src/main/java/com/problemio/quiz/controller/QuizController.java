package com.problemio.quiz.controller;

import com.problemio.global.auth.CustomUserDetails;
import com.problemio.global.common.ApiResponse;
import com.problemio.global.exception.BusinessException;
import com.problemio.global.exception.ErrorCode;
import com.problemio.quiz.dto.QuizCreateRequest;
import com.problemio.quiz.dto.QuestionResponse;
import com.problemio.quiz.dto.QuizResponse;
import com.problemio.quiz.dto.QuizSummaryDto;
import com.problemio.quiz.dto.QuizUpdateRequest;
import com.problemio.quiz.service.QuizService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/quizzes")
@RequiredArgsConstructor
public class QuizController {

    // 퀴즈 서비스 주입
    private final QuizService quizService;

    /**
     * 퀴즈 목록 조회
     * - 페이징(page, size), 정렬(sort), 검색(keyword) 적용
     * - 목록 및 메타 정보 반환
     */
    @GetMapping
    public ResponseEntity<ApiResponse<Map<String, Object>>> getQuizzes(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(defaultValue = "latest") String sort,
            @RequestParam(required = false) String keyword
    ) {
        return ResponseEntity.ok(
                ApiResponse.success(quizService.getQuizzes(page, size, sort, keyword))
        );
    }

    /**
     * 퀴즈 생성
     */
    @PostMapping
    public ResponseEntity<ApiResponse<QuizResponse>> createQuiz(
            @RequestBody @Valid QuizCreateRequest request,
            @AuthenticationPrincipal CustomUserDetails userDetails
    ) {
        Long userId = requireLogin(userDetails);
        return ResponseEntity.ok(
                ApiResponse.success(quizService.createQuiz(userId, request))
        );
    }

    /**
     * 퀴즈 전체 수정 (PUT)
     */
    @PutMapping("/{quizId}")
    public ResponseEntity<ApiResponse<QuizResponse>> updateQuiz(
            @PathVariable Long quizId,
            @RequestBody @Valid QuizUpdateRequest request,
            @AuthenticationPrincipal CustomUserDetails userDetails
    ) {
        Long userId = requireLogin(userDetails);
        return ResponseEntity.ok(
                ApiResponse.success(quizService.updateQuiz(userId, quizId, request))
        );
    }

    /**
     * 퀴즈 부분 수정 (PATCH)
     */
    @PatchMapping("/{quizId}")
    public ResponseEntity<ApiResponse<QuizResponse>> updateQuizPatch(
            @PathVariable Long quizId,
            @RequestBody QuizUpdateRequest request,
            @AuthenticationPrincipal CustomUserDetails userDetails
    ) {
        Long userId = requireLogin(userDetails);
        return ResponseEntity.ok(
                ApiResponse.success(quizService.updateQuiz(userId, quizId, request))
        );
    }

    /**
     * 퀴즈 삭제
     */
    @DeleteMapping("/{quizId}")
    public ResponseEntity<ApiResponse<Void>> deleteQuiz(
            @PathVariable Long quizId,
            @AuthenticationPrincipal CustomUserDetails userDetails
    ) {
        Long userId = requireLogin(userDetails);
        quizService.deleteQuiz(userId, quizId);
        return ResponseEntity.ok(ApiResponse.success(null));
    }

    /**
     * 퀴즈 상세 조회
     * - 좋아요 여부, 접근 권한 등 포함 확인
     */
    @GetMapping("/{quizId}")
    public ResponseEntity<ApiResponse<QuizResponse>> getQuiz(
            @PathVariable Long quizId,
            @AuthenticationPrincipal CustomUserDetails userDetails
    ) {
        Long viewerId = userDetails != null ? userDetails.getUser().getId() : null;
        return ResponseEntity.ok(
                ApiResponse.success(quizService.getQuiz(quizId, viewerId))
        );
    }

    /**
     * 공개 퀴즈 목록 조회 (비로그인 허용)
     */
    @GetMapping("/public")
    public ResponseEntity<ApiResponse<List<QuizSummaryDto>>> getPublicQuizzes() {
        return ResponseEntity.ok(
                ApiResponse.success(quizService.getPublicQuizzes())
        );
    }

    /**
     * 내 퀴즈 목록 조회
     */
    @GetMapping("/me")
    public ResponseEntity<ApiResponse<List<QuizSummaryDto>>> getMyQuizzes(
            @AuthenticationPrincipal CustomUserDetails userDetails
    ) {
        return ResponseEntity.ok(
                ApiResponse.success(quizService.getUserQuizzes(requireLogin(userDetails)))
        );
    }

    /**
     * 퀴즈 좋아요
     */
    @PostMapping("/{quizId}/like")
    public ResponseEntity<ApiResponse<Map<String, Object>>> likeQuiz(
            @PathVariable Long quizId,
            @AuthenticationPrincipal CustomUserDetails userDetails
    ) {
        quizService.likeQuiz(requireLogin(userDetails), quizId);
        return ResponseEntity.ok(
                ApiResponse.success(Map.of("liked", true))
        );
    }

    /**
     * 퀴즈 좋아요 취소
     */
    @DeleteMapping("/{quizId}/like")
    public ResponseEntity<ApiResponse<Map<String, Object>>> unlikeQuiz(
            @PathVariable Long quizId,
            @AuthenticationPrincipal CustomUserDetails userDetails
    ) {
        quizService.unlikeQuiz(requireLogin(userDetails), quizId);
        return ResponseEntity.ok(
                ApiResponse.success(Map.of("liked", false))
        );
    }

    /**
     * 퀴즈 문제 조회 (랜덤 순서, 개수 제한)
     */
    @GetMapping("/{quizId}/questions")
    public ResponseEntity<ApiResponse<List<QuestionResponse>>> getQuizQuestions(
            @PathVariable Long quizId,
            @RequestParam(required = false) Integer limit,
            @AuthenticationPrincipal CustomUserDetails userDetails
    ) {
        Long viewerId = userDetails != null ? userDetails.getUser().getId() : null;
        return ResponseEntity.ok(
                ApiResponse.success(quizService.getQuizQuestions(quizId, viewerId, limit))
        );
    }

    private Long requireLogin(CustomUserDetails userDetails) {
        if (userDetails == null) {
            throw new BusinessException(ErrorCode.LOGIN_REQUIRED);
        }
        return userDetails.getUser().getId();
    }
}
