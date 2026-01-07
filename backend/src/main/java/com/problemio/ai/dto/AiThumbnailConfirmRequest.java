package com.problemio.ai.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class AiThumbnailConfirmRequest {

    @NotBlank
    private String candidateId;
}
