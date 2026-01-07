package com.problemio.comment.dto;

import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class CommentDeleteRequest {

    // 게스트 삭제 시 비밀번호 필요
    @Size(min = 4, max = 100)
    private String password;
}
