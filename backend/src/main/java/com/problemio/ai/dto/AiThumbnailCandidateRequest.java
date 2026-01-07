package com.problemio.ai.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class AiThumbnailCandidateRequest {

    @NotBlank
    @Size(min = 5, max = 100)
    private String title;

    @NotBlank
    @Size(min = 10, max = 500)
    private String description;
}
