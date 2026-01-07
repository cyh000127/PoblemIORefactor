package com.problemio.comment.domain;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class Comment {
    private Long id;
    private Long quizId;
    private Long parentCommentId;
    private Long rootCommentId;

    private Long userId;              // 회원이면 값, 게스트면 null
    private String guestNickname;     // 게스트면 값, 회원이면 null
    private String guestPasswordHash; // 절대 응답으로 노출 X
    private String writerIp;

    private String content;
    private Integer likeCount;

    private boolean deleted;

    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
