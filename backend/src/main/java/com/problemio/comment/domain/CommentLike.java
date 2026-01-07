package com.problemio.comment.domain;

import lombok.Data;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

@Data
public class CommentLike {

    private Long userId;
    private Long commentId;

    private LocalDateTime createdAt;
}
