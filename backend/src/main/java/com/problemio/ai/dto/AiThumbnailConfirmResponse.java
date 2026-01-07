package com.problemio.ai.dto;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class AiThumbnailConfirmResponse {

    private final String thumbnailUrl;
}
