package com.problemio.comment.service;

import com.problemio.comment.dto.CommentCreateRequest;
import com.problemio.comment.dto.CommentResponse;
import com.problemio.comment.dto.CommentUpdateRequest;

import java.util.List;

public interface CommentService {

    /**
     * 댓글 생성
     */
    void createComment(Long quizId, Long userId, CommentCreateRequest request, String writerIp);

    /**
     * 댓글 수정 (작성자만 가능)
     */
    void updateComment(Long commentId, Long userId, CommentUpdateRequest request);

    /**
     * 댓글 삭제 (소프트 삭제, 작성자 또는 관리자 가능)
     */
    void deleteComment(Long commentId, com.problemio.user.domain.User user, String guestPassword);

    /**
     * 퀴즈별 댓글 목록 조회 (페이징)
     *
     * @param quizId 대상 퀴즈 ID
     * @param userId 현재 로그인 유저 ID (비로그인일 경우 null 가능)
     * @param page   1부터 시작하는 페이지 번호
     * @param size   페이지 크기
     */
    List<CommentResponse> getComments(Long quizId, Long userId, int page, int size);

    /**
     * 특정 댓글의 대댓글 조회 (정렬: 작성순)
     */
    List<CommentResponse> getReplies(Long parentCommentId, Long userId);

    /**
     * 댓글 좋아요 토글
     */
    void toggleLike(Long commentId, Long userId);
}
