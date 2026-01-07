package com.problemio.quiz.dto;

import com.problemio.quiz.dto.QuestionCreateRequest;

import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
public class QuizCreateRequest {

    @NotBlank
    private String title;

    @NotBlank
    private String description;

    @NotBlank
    private String thumbnailUrl;

    // 문제 공개 여부
    private boolean isPublic = true;

    //  문제 리스트
    // 프론트에서 없는 상태로 보내면 null이거나 빈 리스트니까
    // 서비스에서 NPE 안나게 체크해야 함
    private List<QuestionCreateRequest> questions;
}
