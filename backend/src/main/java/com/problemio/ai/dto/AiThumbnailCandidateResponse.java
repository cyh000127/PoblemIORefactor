package com.problemio.ai.dto;

import lombok.Builder;
import lombok.Getter;

import java.util.List;

@Getter
@Builder
public class AiThumbnailCandidateResponse {

    private final List<Candidate> candidates;

    @Getter
    @Builder
    public static class Candidate {
        private final String candidateId;
        private final String previewDataUrl;
    }
}
