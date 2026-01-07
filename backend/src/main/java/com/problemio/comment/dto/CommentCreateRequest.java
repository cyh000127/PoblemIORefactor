package com.problemio.comment.dto;

import com.fasterxml.jackson.annotation.JsonAlias;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;



@Data
public class CommentCreateRequest {
    @JsonAlias({"parentCommentId", "parent_comment_id"})
    private Long parentCommentId;

    @NotBlank
    @Size(max = 500)
    private String content;

    // 게스트용 필드 (회원일 때는 무시)
    @Size(max = 50)
    private String nickname;

    @Size(min = 4, max = 100)
    private String password;
}
