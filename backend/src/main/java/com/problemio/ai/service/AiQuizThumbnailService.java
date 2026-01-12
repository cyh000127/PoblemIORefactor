package com.problemio.ai.service;

import com.problemio.ai.cache.CandidateCache;
import com.problemio.ai.dto.AiThumbnailCandidateResponse;
import com.problemio.ai.dto.AiThumbnailConfirmResponse;
import com.problemio.global.exception.BusinessException;
import com.problemio.global.exception.ErrorCode;
import com.problemio.global.service.LocalFileService;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.Base64;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class AiQuizThumbnailService {

    private final CandidateCache candidateCache;
    private final GmsGeminiClient gmsGeminiClient;
    private final LocalFileService localFileService;

    public AiThumbnailCandidateResponse generateCandidates(String title, String description) {
        String safeTitle = title == null ? "" : title.trim();
        String safeDescription = description == null ? "" : description.trim();

        if (safeTitle.length() < 5 || safeDescription.length() < 10) {
            throw new BusinessException(ErrorCode.INVALID_INPUT_VALUE);
        }

        List<String> styleHints = List.of(
                "Cinematic illustration style.\nDark background with a central symbolic object illuminated by a spotlight.\nSemi-detailed rendering with depth and atmosphere.\nMoody, premium, game-cover-like aesthetic.",
                "Vibrant modern illustration.\nDynamic composition with multiple topic-related objects.\nBright colors, motion, and playful energy.\nFun but polished quiz-game aesthetic.");
        List<AiThumbnailCandidateResponse.Candidate> candidates = styleHints.stream()
                .map(style -> createCandidate(safeTitle, safeDescription, style))
                .toList();

        return AiThumbnailCandidateResponse.builder()
                .candidates(candidates)
                .build();
    }

    public AiThumbnailConfirmResponse confirmCandidate(String candidateId, Long userId) {
        if (candidateId == null || candidateId.isBlank()) {
            throw new BusinessException(ErrorCode.INVALID_INPUT_VALUE);
        }

        // 디버그 로그
        System.out.println("Confirming Candidate ID: " + candidateId);

        byte[] bytes = candidateCache.get(candidateId);
        if (bytes == null) {
            System.out.println("Cache MISS for ID: " + candidateId);
            throw new BusinessException(ErrorCode.INVALID_INPUT_VALUE);
        }
        System.out.println("Cache HIT for ID: " + candidateId + ", size: " + bytes.length);

        String key = "quiz-thumbnails/" + userId + "/" + candidateId + ".png";
        String fileKey = localFileService.uploadBytes(bytes, key, "image/png");
        candidateCache.evict(candidateId);

        String fullUrl = buildFullUrl(fileKey);
        return AiThumbnailConfirmResponse.builder()
                .thumbnailUrl(fullUrl)
                .build();
    }

    private AiThumbnailCandidateResponse.Candidate createCandidate(
            String title,
            String description,
            String styleHint) {
        byte[] bytes = gmsGeminiClient.generatePngBytes(title, description, styleHint);
        String candidateId = UUID.randomUUID().toString();

        // 디버그 로그
        System.out.println(
                "Generated Candidate ID: " + candidateId + ", bytes: " + (bytes != null ? bytes.length : "null"));

        candidateCache.put(candidateId, bytes);

        String previewDataUrl = "data:image/png;base64," + Base64.getEncoder().encodeToString(bytes);
        return AiThumbnailCandidateResponse.Candidate.builder()
                .candidateId(candidateId)
                .previewDataUrl(previewDataUrl)
                .build();
    }

    private String buildFullUrl(String fileKey) {
        String key = fileKey.startsWith("/") ? fileKey.substring(1) : fileKey;
        return "/uploads/" + key;
    }
}
