package com.problemio.comment.dto;

import lombok.*;
import java.time.LocalDateTime;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.problemio.global.config.S3UrlSerializer;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CommentResponse {

    private Long id;
    private Long quizId;
    private Long parentCommentId;
    private Long rootCommentId;

    // 작성자 정보 (회원/게스트 통합 응답)
    private Long userId;            // 회원이면 값, 게스트면 null
    private String nickname;        // 회원: users.nickname, 게스트: guest_nickname


    @JsonSerialize(using = S3UrlSerializer.class)
    private String profileImageUrl; // 회원만 존재, 게스트는 null

    private String content;
    private Integer likeCount;
    private Integer replyCount;

    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    // 클라이언트 편의를 위한 플래그들
    private boolean mine;       // 현재 로그인 유저가 쓴 댓글인지
    private boolean likedByMe;  // 현재 로그인 유저가 좋아요 눌렀는지
}
