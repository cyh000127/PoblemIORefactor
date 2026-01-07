package com.problemio.ai.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.problemio.global.exception.BusinessException;
import com.problemio.global.exception.ErrorCode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.http.*;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

import java.util.Base64;
import java.util.List;
import java.util.Map;

@Component
public class GmsGeminiClient {

    private static final Logger log = LoggerFactory.getLogger(GmsGeminiClient.class);

    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper;

    @Value("${gms.base-url:https://gms.ssafy.io/gmsapi/}")
    private String baseUrl;

    @Value("${gms.api-key}")
    private String apiKey;

    @Value("${gms.model:gemini-2.0-flash-exp-image-generation}")
    private String model;

    public GmsGeminiClient(RestTemplateBuilder restTemplateBuilder, ObjectMapper objectMapper) {
        this.restTemplate = restTemplateBuilder.build();
        this.objectMapper = objectMapper;
    }

    public byte[] generatePngBytes(String title, String description, String styleHint) {
        String prompt = buildPrompt(title, description, styleHint);
        return requestImage(prompt);
    }

    private byte[] requestImage(String prompt) {
        log.info("GMS Gemini request baseUrl={} model={}", baseUrl, model);
        String endpoint = buildEndpoint();

        // Gemini 2.0 Flash 이미지 생성 페이로드
        // https://gms.ssafy.io/gmsapi/generativelanguage.googleapis.com/v1beta/models/gemini-2.0-flash-exp-image-generation:generateContent
        Map<String, Object> payload = Map.of(
                "contents", List.of(
                        Map.of("parts", List.of(
                                Map.of("text", prompt)))),
                "generationConfig", Map.of(
                        "responseModalities", List.of("Text", "Image")));

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);

        // GMS: Gemini 모델은 쿼리 파라미터 'key' 사용
        String url = UriComponentsBuilder.fromHttpUrl(endpoint)
                .queryParam("key", apiKey)
                .toUriString();

        try {
            String requestBody = objectMapper.writeValueAsString(payload);
            log.info("Request Body: {}", requestBody);

            HttpEntity<String> entity = new HttpEntity<>(requestBody, headers);
            ResponseEntity<String> response = restTemplate.exchange(
                    url,
                    HttpMethod.POST,
                    entity,
                    String.class);

            if (!response.getStatusCode().is2xxSuccessful() || response.getBody() == null) {
                log.error("GMS Error Status: {} Body: {}", response.getStatusCode(), response.getBody());
                throw new BusinessException(ErrorCode.INTERNAL_SERVER_ERROR);
            }

            return extractImageBytes(response.getBody());

        } catch (Exception e) {
            log.error("Failed to generate image via GMS Gemini", e);
            throw new BusinessException(ErrorCode.INTERNAL_SERVER_ERROR);
        }
    }

    private byte[] extractImageBytes(String responseBody) throws Exception {
        JsonNode root = objectMapper.readTree(responseBody);

        // Gemini 응답 구조:
        // candidates[0].content.parts[0].inlineData.data (Base64)
        // 또는 candidates[0].content.parts[0].inline_data.data (Base64)

        JsonNode candidates = root.get("candidates");
        if (candidates != null && candidates.isArray() && !candidates.isEmpty()) {
            JsonNode firstCandidate = candidates.get(0);
            JsonNode parts = firstCandidate.path("content").path("parts");

            if (parts.isArray() && !parts.isEmpty()) {
                for (JsonNode part : parts) {
                    JsonNode inlineData = part.get("inlineData");
                    if (inlineData == null) {
                        inlineData = part.get("inline_data");
                    }

                    if (inlineData != null && inlineData.has("data")) {
                        String base64Data = inlineData.get("data").asText();
                        return Base64.getDecoder().decode(base64Data);
                    }
                }
            }
        }

        log.error("No image data found in response: {}", responseBody);
        throw new BusinessException(ErrorCode.INTERNAL_SERVER_ERROR);
    }

    private String buildEndpoint() {
        // gms.base-url에 전체 경로가 포함될 수 있음 (예: .../models/)

        String base = baseUrl.endsWith("/") ? baseUrl : baseUrl + "/";
        String targetPath = "generativelanguage.googleapis.com/v1beta/models/";

        if (base.contains(targetPath)) {
            // Base URL에 타겟 경로가 포함된 경우 모델과 메서드만 추가
            return base + model + ":generateContent";
        }

        // 그렇지 않으면 전체 경로 추가
        return base + targetPath + model + ":generateContent";
    }

    private String buildPrompt(String title, String description, String styleHint) {
        return String.format(
                "You are generating an image, not text.\n\n" +
                        "Output requirements (STRICT):\n" +
                        "- Generate exactly ONE image.\n" +
                        "- The image must be a PNG.\n" +
                        "- Respond with ONLY the raw base64-encoded PNG bytes.\n" +
                        "- Do NOT include explanations, markdown, code fences, data URLs, or any extra characters.\n" +
                        "- The response must be valid base64 and nothing else.\n\n" +
                        "Image purpose:\n" +
                        "- This image is a representative thumbnail for a quiz or trivia game.\n" +
                        "- It is NOT a question image and must not contain clues or readable information.\n\n" +
                        "Image requirements:\n" +
                        "- Square aspect ratio (1:1), suitable for a quiz thumbnail.\n" +
                        "- Clean, uncluttered composition with strong visual contrast.\n" +
                        "- Easy to recognize at small sizes.\n" +
                        "- Do NOT include any text, letters, numbers, logos, watermarks, or signatures.\n" +
                        "- Do NOT depict real people, identifiable faces, or specific individuals.\n" +
                        "- Do NOT include copyrighted characters, brand logos, or trademarks.\n\n" +
                        "Conceptual theme:\n" +
                        "- Create an artistic illustration representing the quiz topic.\n" +
                        "- Visualize the subject matter using characteristic objects, props, or environments\n" +
                        "  that are strongly associated with the topic.\n" +
                        "- Avoid generic avatars, profile icons, or placeholder silhouettes.\n" +
                        "- Focus on symbolic scenes or compositions that suggest the topic at a glance.\n" +
                        "- Subject-related elements must be generic and non-branded.\n\n" +
                        "Visual style:\n" +
                        "- Modern illustrative style (not flat UI icons, not sticker art).\n" +
                        "- High contrast lighting with a dramatic or playful tone.\n" +
                        "- Polished, professional thumbnail aesthetic suitable for a quiz or trivia app.\n\n" +
                        "Quiz context (for inspiration only, do NOT render as text):\n" +
                        "Title: %s\n" +
                        "Description: %s\n\n" +
                        "Style variation (choose ONE and follow it strictly):\n" +
                        "%s",
                title, description, styleHint);
    }
}
