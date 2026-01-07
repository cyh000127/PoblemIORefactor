package com.problemio.comment.service;

import com.problemio.comment.domain.Comment;
import com.problemio.comment.domain.CommentLike;
import com.problemio.comment.dto.CommentCreateRequest;
import com.problemio.comment.dto.CommentResponse;
import com.problemio.comment.dto.CommentUpdateRequest;
import com.problemio.comment.mapper.CommentLikeMapper;
import com.problemio.comment.mapper.CommentMapper;
import com.problemio.global.exception.BusinessException;
import com.problemio.global.exception.ErrorCode;
import com.problemio.quiz.mapper.QuizMapper;
import com.problemio.user.dto.UserResponse;
import com.problemio.user.mapper.UserMapper;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.LocalDateTime;
import com.problemio.global.util.TimeUtils;

import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class CommentServiceImpl implements CommentService {

    private final CommentMapper commentMapper;
    private final CommentLikeMapper commentLikeMapper;
    private final UserMapper userMapper;
    private final QuizMapper quizMapper;
    private final PasswordEncoder passwordEncoder;

    @Override
    @Transactional
    public void createComment(Long quizId, Long userId, CommentCreateRequest request, String writerIp) {
        if (quizMapper.findById(quizId).isEmpty()) {
            throw new BusinessException(ErrorCode.QUIZ_NOT_FOUND);
        }

        if (request.getContent() == null || request.getContent().isBlank()) {
            throw new BusinessException(ErrorCode.COMMENT_CONTENT_REQUIRED);
        }

        Comment comment = new Comment();
        comment.setQuizId(quizId);
        comment.setUserId(userId);
        comment.setParentCommentId(request.getParentCommentId());

        if (userId == null) {
            if (request.getNickname() == null || request.getNickname().isBlank()) {
                throw new BusinessException(ErrorCode.COMMENT_NICKNAME_REQUIRED);
            }
            if (request.getPassword() == null || request.getPassword().isBlank()) {
                throw new BusinessException(ErrorCode.COMMENT_PASSWORD_REQUIRED);
            }

            comment.setGuestNickname(request.getNickname());
            comment.setGuestPasswordHash(passwordEncoder.encode(request.getPassword()));
        }

        // 상위/루트 댓글 처리
        if (request.getParentCommentId() != null) {
            Comment parent = commentMapper.findById(request.getParentCommentId());
            if (parent == null || parent.isDeleted()) {
                throw new BusinessException(ErrorCode.COMMENT_NOT_FOUND);
            }
            if (!Objects.equals(parent.getQuizId(), quizId)) {
                throw new BusinessException(ErrorCode.COMMENT_FORBIDDEN);
            }
            Long rootId = parent.getRootCommentId() != null ? parent.getRootCommentId() : parent.getId();
            comment.setRootCommentId(rootId);
        }

        comment.setContent(request.getContent());
        comment.setLikeCount(0);
        comment.setWriterIp(writerIp);
        comment.setDeleted(false);
        // Time injection
        LocalDateTime now = TimeUtils.now();
        comment.setCreatedAt(now);
        comment.setUpdatedAt(now);

        commentMapper.insertComment(comment);

        // 루트 댓글: root_comment_id를 자신의 ID로 설정
        if (request.getParentCommentId() == null) {
            commentMapper.updateRootCommentId(comment.getId(), comment.getId());
        }
    }

    @Override
    @Transactional
    public void updateComment(Long commentId, Long userId, CommentUpdateRequest request) {
        Comment existing = commentMapper.findById(commentId);
        if (existing == null || existing.isDeleted()) {
            throw new BusinessException(ErrorCode.COMMENT_NOT_FOUND);
        }

        if (existing.getUserId() != null) {
            if (userId == null || !existing.getUserId().equals(userId)) {
                throw new BusinessException(ErrorCode.COMMENT_FORBIDDEN);
            }
        } else {
            // 게스트 댓글 검증
            if (request.getPassword() == null || request.getPassword().isBlank()) {
                throw new BusinessException(ErrorCode.COMMENT_PASSWORD_REQUIRED);
            }
            if (!passwordEncoder.matches(request.getPassword(), existing.getGuestPasswordHash())) {
                throw new BusinessException(ErrorCode.COMMENT_PASSWORD_MISMATCH);
            }
        }

        existing.setContent(request.getContent());
        existing.setUpdatedAt(TimeUtils.now());
        commentMapper.updateComment(existing);
    }

    @Override
    @Transactional
    public void deleteComment(Long commentId, com.problemio.user.domain.User user, String guestPassword) {
        Comment existing = commentMapper.findById(commentId);
        if (existing == null || existing.isDeleted()) {
            throw new BusinessException(ErrorCode.COMMENT_NOT_FOUND);
        }

        if (existing.getUserId() != null) {
            // 회원 댓글 권한 검사
            if (user == null) {
                throw new BusinessException(ErrorCode.COMMENT_FORBIDDEN); // 타인 댓글 삭제 시도 (비로그인)
            }

            // 작성자 또는 관리자가 아니면 거부
            boolean isOwner = existing.getUserId().equals(user.getId());
            boolean isAdmin = "ROLE_ADMIN".equals(user.getRole());

            if (!isOwner && !isAdmin) {
                throw new BusinessException(ErrorCode.COMMENT_FORBIDDEN);
            }
        } else {
            // 게스트 댓글 권한 검사
            // 관리자: 비밀번호 없이 삭제 가능
            boolean isAdmin = user != null && "ROLE_ADMIN".equals(user.getRole());

            if (!isAdmin) {
                if (guestPassword == null || guestPassword.isBlank()) {
                    throw new BusinessException(ErrorCode.COMMENT_PASSWORD_REQUIRED);
                }
                if (!passwordEncoder.matches(guestPassword, existing.getGuestPasswordHash())) {
                    throw new BusinessException(ErrorCode.COMMENT_PASSWORD_MISMATCH);
                }
            }
        }

        // 물리 삭제 대신 논리 삭제(Soft Delete)
        commentMapper.softDeleteComment(commentId, TimeUtils.now());
    }

    @Override
    @Transactional(readOnly = true)
    public List<CommentResponse> getComments(Long quizId, Long userId, int page, int size) {
        int pageSafe = Math.max(page, 1);
        int sizeSafe = Math.max(size, 1);
        int offset = (pageSafe - 1) * sizeSafe;

        List<Comment> comments = commentMapper.findRootCommentsByQuizId(quizId, sizeSafe, offset);

        List<Long> writerIds = comments.stream()
                .map(Comment::getUserId)
                .filter(Objects::nonNull)
                .distinct()
                .toList();

        Map<Long, UserResponse> writers = writerIds.isEmpty()
                ? Collections.emptyMap()
                : userMapper.findByIds(writerIds).stream()
                .collect(Collectors.toMap(UserResponse::getId, Function.identity()));

        List<Long> rootIds = comments.stream()
                .map(Comment::getId)
                .toList();

        Set<Long> likedIds = (userId != null && !rootIds.isEmpty())
                ? new HashSet<>(commentLikeMapper.findLikedCommentIds(userId, rootIds))
                : Collections.emptySet();

        Map<Long, Integer> replyCounts = rootIds.isEmpty()
                ? Collections.emptyMap()
                : commentMapper.countRepliesByParentIds(rootIds).stream()
                .collect(Collectors.toMap(CommentMapper.CommentReplyCount::getParentId, CommentMapper.CommentReplyCount::getCount));

        return comments.stream()
                .map(comment -> {
                    UserResponse writer = comment.getUserId() != null
                            ? writers.get(comment.getUserId())
                            : null;

                    String nickname = writer != null ? writer.getNickname() : comment.getGuestNickname();
                    String profileImage = writer != null ? writer.getProfileImageUrl() : null;

                    boolean isOwner = userId != null && comment.getUserId() != null && userId.equals(comment.getUserId());
                    boolean likedByMe = likedIds.contains(comment.getId());

                    return CommentResponse.builder()
                            .id(comment.getId())
                            .quizId(comment.getQuizId())
                            .userId(comment.getUserId())
                            .parentCommentId(comment.getParentCommentId())
                            .rootCommentId(comment.getRootCommentId())
                            .nickname(nickname)
                            .profileImageUrl(profileImage)

                            .content(comment.getContent())
                            .likeCount(comment.getLikeCount())
                            .replyCount(replyCounts.getOrDefault(comment.getId(), 0))

                            .mine(isOwner)
                            .likedByMe(likedByMe)

                            .createdAt(comment.getCreatedAt())
                            .updatedAt(comment.getUpdatedAt())
                            .build();
                })
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<CommentResponse> getReplies(Long parentCommentId, Long userId) {
        Comment parent = commentMapper.findById(parentCommentId);
        if (parent == null || parent.isDeleted()) {
            throw new BusinessException(ErrorCode.COMMENT_NOT_FOUND);
        }

        List<Comment> comments = commentMapper.findRepliesByParentId(parentCommentId);

        List<Long> writerIds = comments.stream()
                .map(Comment::getUserId)
                .filter(Objects::nonNull)
                .distinct()
                .toList();

        Map<Long, UserResponse> writers = writerIds.isEmpty()
                ? Collections.emptyMap()
                : userMapper.findByIds(writerIds).stream()
                .collect(Collectors.toMap(UserResponse::getId, Function.identity()));

        List<Long> ids = comments.stream().map(Comment::getId).toList();
        Set<Long> likedIds = (userId != null && !ids.isEmpty())
                ? new HashSet<>(commentLikeMapper.findLikedCommentIds(userId, ids))
                : Collections.emptySet();

        Map<Long, Integer> replyCounts = ids.isEmpty()
                ? Collections.emptyMap()
                : commentMapper.countRepliesByParentIds(ids).stream()
                .collect(Collectors.toMap(CommentMapper.CommentReplyCount::getParentId, CommentMapper.CommentReplyCount::getCount));

        return comments.stream()
                .map(comment -> {
                    UserResponse writer = comment.getUserId() != null
                            ? writers.get(comment.getUserId())
                            : null;

                    String nickname = writer != null ? writer.getNickname() : comment.getGuestNickname();
                    String profileImage = writer != null ? writer.getProfileImageUrl() : null;

                    boolean isOwner = userId != null && comment.getUserId() != null && userId.equals(comment.getUserId());
                    boolean likedByMe = likedIds.contains(comment.getId());

                    return CommentResponse.builder()
                            .id(comment.getId())
                            .quizId(comment.getQuizId())
                            .parentCommentId(comment.getParentCommentId())
                            .rootCommentId(comment.getRootCommentId())
                            .userId(comment.getUserId())
                            .nickname(nickname)
                            .profileImageUrl(profileImage)
                            .content(comment.getContent())
                            .likeCount(comment.getLikeCount())
                            .replyCount(replyCounts.getOrDefault(comment.getId(), 0))
                            .mine(isOwner)
                            .likedByMe(likedByMe)
                            .createdAt(comment.getCreatedAt())
                            .updatedAt(comment.getUpdatedAt())
                            .build();
                })
                .toList();
    }


    @Override
    @Transactional
    public void toggleLike(Long commentId, Long userId) {
        if (userId == null) {
            throw new BusinessException(ErrorCode.LOGIN_REQUIRED);
        }

        Comment comment = commentMapper.findById(commentId);
        if (comment == null || comment.isDeleted()) {
            throw new BusinessException(ErrorCode.COMMENT_NOT_FOUND);
        }

        boolean alreadyLiked = commentLikeMapper.exists(userId, commentId);

        if (alreadyLiked) {
            // 좋아요 취소
            commentLikeMapper.delete(userId, commentId);
            commentMapper.decreaseLikeCount(commentId);
        } else {
            // 좋아요 등록
            CommentLike like = new CommentLike();
            like.setUserId(userId);
            like.setCommentId(commentId);
            commentLikeMapper.insert(like);
            commentMapper.increaseLikeCount(commentId);
        }
    }
}
