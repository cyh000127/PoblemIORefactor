package com.problemio.comment.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class CommentUpdateRequest {

    @NotBlank
    @Size(max = 500)
    private String content;

    // 게스트일 경우 필요
    @Size(min = 4, max = 100)
    private String password;
}
