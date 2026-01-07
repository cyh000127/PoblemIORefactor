package com.problemio.comment.controller;

import com.problemio.comment.dto.CommentCreateRequest;
import com.problemio.comment.dto.CommentDeleteRequest;
import com.problemio.comment.dto.CommentResponse;
import com.problemio.comment.dto.CommentUpdateRequest;
import com.problemio.comment.service.CommentService;
import com.problemio.global.auth.CustomUserDetails;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api")
public class CommentController {

    private final CommentService commentService;

    @PostMapping("/quizzes/{quizId}/comments")
    public void createComment(
            @PathVariable Long quizId,
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @Valid @RequestBody CommentCreateRequest request,
            HttpServletRequest httpRequest
    ) {
        Long userId = userDetails != null ? userDetails.getUser().getId() : null;
        commentService.createComment(quizId, userId, request, httpRequest.getRemoteAddr());
    }

    @PatchMapping("/comments/{commentId}")
    public void updateComment(
            @PathVariable Long commentId,
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @Valid @RequestBody CommentUpdateRequest request
    ) {
        Long userId = userDetails != null ? userDetails.getUser().getId() : null;
        commentService.updateComment(commentId, userId, request);
    }

    @DeleteMapping("/comments/{commentId}")
    public void deleteComment(
            @PathVariable Long commentId,
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @Valid @RequestBody(required = false) CommentDeleteRequest request
    ) {
        com.problemio.user.domain.User user = userDetails != null ? userDetails.getUser() : null;
        String guestPassword = request != null ? request.getPassword() : null;
        commentService.deleteComment(commentId, user, guestPassword);
    }

    @GetMapping("/quizzes/{quizId}/comments")
    public List<CommentResponse> getComments(
            @PathVariable Long quizId,
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int size
    ) {
        Long userId = userDetails != null ? userDetails.getUser().getId() : null;
        return commentService.getComments(quizId, userId, page, size);
    }

    @GetMapping("/comments/{commentId}/replies")
    public List<CommentResponse> getReplies(
            @PathVariable Long commentId,
            @AuthenticationPrincipal CustomUserDetails userDetails
    ) {
        Long userId = userDetails != null ? userDetails.getUser().getId() : null;
        return commentService.getReplies(commentId, userId);
    }

    @PostMapping("/comments/{commentId}/likes")
    public void toggleLike(
            @PathVariable Long commentId,
            @AuthenticationPrincipal CustomUserDetails userDetails
    ) {
        Long userId = userDetails != null ? userDetails.getUser().getId() : null;
        commentService.toggleLike(commentId, userId);
    }
}
