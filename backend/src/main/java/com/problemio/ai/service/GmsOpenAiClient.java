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
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Component
public class GmsOpenAiClient {

    private static final Logger log = LoggerFactory.getLogger(GmsOpenAiClient.class);
    private static final Pattern DATA_URL_PATTERN =
            Pattern.compile("data:image/[^;]+;base64,([A-Za-z0-9+/=\\s]+)");
    private static final Pattern BASE64_BLOCK_PATTERN =
            Pattern.compile("([A-Za-z0-9+/=]{200,})");

    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper;

    @Value("${gms.base-url}")
    private String baseUrl;

    @Value("${gms.api-key}")
    private String apiKey;

    @Value("${gms.model:gpt-4o}")
    private String model;

    public GmsOpenAiClient(RestTemplateBuilder restTemplateBuilder, ObjectMapper objectMapper) {
        this.restTemplate = restTemplateBuilder.build();
        this.objectMapper = objectMapper;
    }

    public byte[] generatePngBytes(String title, String description, String styleHint) {
        String prompt = buildPrompt(title, description, styleHint);
        String base64 = requestImageBase64(prompt);
        try {
            return Base64.getDecoder().decode(base64);
        } catch (IllegalArgumentException e) {
            throw new BusinessException(ErrorCode.INTERNAL_SERVER_ERROR);
        }
    }

    private String requestImageBase64(String prompt) {
        log.info("GMS request baseUrl={} model={} apiKeyPresent={} promptLen={}",
                baseUrl,
                model,
                apiKey != null && !apiKey.isBlank(),
                prompt != null ? prompt.length() : 0);
        for (int attempt = 0; attempt < 2; attempt++) {
            try {
                HttpHeaders headers = new HttpHeaders();
                headers.setContentType(MediaType.APPLICATION_JSON);

                Map<String, Object> payload = Map.of(
                        "contents", List.of(
                                Map.of("parts", List.of(
                                        Map.of("text", prompt)
                                ))
                        ),
                        "generationConfig", Map.of(
                                "responseModalities", List.of("IMAGE")
                        )
                );

                String endpoint = buildEndpoint(baseUrl, model);
                String url = UriComponentsBuilder.fromHttpUrl(endpoint)
                        .queryParam("key", apiKey)
                        .toUriString();

                if (attempt == 0) {
                    log.info("GMS request payload={}", truncate(objectMapper.writeValueAsString(payload)));
                }
                HttpEntity<Map<String, Object>> entity = new HttpEntity<>(payload, headers);
                ResponseEntity<String> response = restTemplate.exchange(
                        url,
                        HttpMethod.POST,
                        entity,
                        String.class
                );

                if (!response.getStatusCode().is2xxSuccessful()) {
                    log.warn("GMS response status={} body={}",
                            response.getStatusCode(),
                            truncate(response.getBody()));
                    continue;
                }

                String content = extractContent(response.getBody());
                String base64 = extractBase64(content);
                if (base64 != null) {
                    return base64;
                }
                log.warn("GMS response missing base64. contentPreview={}", truncate(content));
            } catch (Exception e) {
                log.warn("GMS request attempt {} failed: {}", attempt + 1, e.toString());
                // 재시도를 위해 계속 진행
            }
        }

        throw new BusinessException(ErrorCode.INTERNAL_SERVER_ERROR);
    }

    private String extractContent(String body) throws Exception {
        if (body == null || body.isBlank()) {
            throw new BusinessException(ErrorCode.INTERNAL_SERVER_ERROR);
        }

        JsonNode root = objectMapper.readTree(body);
        if (root.has("error")) {
            log.warn("GMS error payload={}", truncate(body));
            throw new BusinessException(ErrorCode.INTERNAL_SERVER_ERROR);
        }

        JsonNode outputText = root.get("output_text");
        if (outputText != null && !outputText.isNull() && !outputText.asText().isBlank()) {
            return outputText.asText();
        }

        JsonNode candidates = root.get("candidates");
        if (candidates != null && candidates.isArray() && !candidates.isEmpty()) {
            for (JsonNode candidate : candidates) {
                JsonNode parts = candidate.path("content").path("parts");
                if (parts != null && parts.isArray()) {
                    for (JsonNode part : parts) {
                        JsonNode inlineData = part.get("inlineData");
                        if (inlineData == null) {
                            inlineData = part.get("inline_data");
                        }
                        if (inlineData != null && inlineData.has("data")) {
                            String data = inlineData.get("data").asText();
                            if (data != null && !data.isBlank()) {
                                return data;
                            }
                        }
                        JsonNode text = part.get("text");
                        if (text != null && !text.isNull() && !text.asText().isBlank()) {
                            return text.asText();
                        }
                    }
                }
            }
        }

        JsonNode output = root.get("output");
        if (output != null && output.isArray() && !output.isEmpty()) {
            JsonNode content = output.get(0).get("content");
            if (content != null && content.isArray() && !content.isEmpty()) {
                JsonNode text = content.get(0).get("text");
                if (text != null && !text.isNull()) {
                    return text.asText();
                }
            }
        }

        JsonNode choices = root.get("choices");
        if (choices == null || !choices.isArray() || choices.isEmpty()) {
            log.warn("GMS response missing choices. bodyPreview={}", truncate(body));
            throw new BusinessException(ErrorCode.INTERNAL_SERVER_ERROR);
        }

        JsonNode message = choices.get(0).get("message");
        JsonNode content = message != null ? message.get("content") : null;
        if (content == null || content.isNull()) {
            throw new BusinessException(ErrorCode.INTERNAL_SERVER_ERROR);
        }
        return content.asText();
    }

    private String extractBase64(String content) {
        if (content == null || content.isBlank()) {
            return null;
        }

        Matcher dataMatcher = DATA_URL_PATTERN.matcher(content);
        if (dataMatcher.find()) {
            return normalizeBase64(dataMatcher.group(1));
        }

        if (content.trim().startsWith("{")) {
            try {
                JsonNode node = objectMapper.readTree(content);
                if (node.has("image_base64")) {
                    return normalizeBase64(node.get("image_base64").asText());
                }
                if (node.has("base64")) {
                    return normalizeBase64(node.get("base64").asText());
                }
                if (node.has("image")) {
                    return normalizeBase64(node.get("image").asText());
                }
            } catch (Exception ignored) {
                // Continue to regex fallback
            }
        }

        Matcher blockMatcher = BASE64_BLOCK_PATTERN.matcher(content);
        if (blockMatcher.find()) {
            return normalizeBase64(blockMatcher.group(1));
        }

        return null;
    }

    private String normalizeBase64(String raw) {
        if (raw == null) return null;
        return raw.replaceAll("\\s+", "");
    }

    private String buildPrompt(String title, String description, String styleHint) {
        return String.join("\n",
                "You are generating an image, not text.",
                "",
                "Output requirements (STRICT):",
                "- Generate exactly ONE image.",
                "- The image must be a PNG.",
                "- Respond with ONLY the raw base64-encoded PNG bytes.",
                "- Do NOT include explanations, markdown, code fences, data URLs, or any extra characters.",
                "- The response must be valid base64 and nothing else.",
                "",
                "Image requirements:",
                "- Square aspect ratio (1:1), suitable for a quiz thumbnail.",
                "- Simple composition, high contrast, easy to recognize at small sizes.",
                "- Do NOT include any text, letters, numbers, logos, watermarks, or signatures.",
                "- Do NOT depict real people, identifiable faces, or specific individuals.",
                "- Do NOT include copyrighted characters, brand logos, or trademarks.",
                "",
                "Conceptual theme:",
                "- An illustration representing a \"quiz\" or \"guessing game\" based on the given topic.",
                "- Use abstract symbols, silhouettes, icons, or conceptual visuals related to the quiz subject.",
                "- Emphasize curiosity, mystery, or challenge (e.g., silhouettes, shadows, spotlight lighting, abstract shapes).",
                "- Subject-related objects or environments may be included, but must be generic and non-branded.",
                "",
                "Visual style:",
                "- Clean, modern illustration style.",
                "- High contrast lighting with a dramatic or playful tone.",
                "- Simple, uncluttered composition.",
                "- Professional, game-like thumbnail aesthetic suitable for a quiz or trivia app.",
                "",
                "Quiz context (for inspiration only, do NOT render as text):",
                "Title: " + title,
                "Description: " + description,
                "",
                "Style variation A:",
                "- Minimal flat illustration.",
                "- Dark background with a single highlighted subject silhouette.",
                "- Strong contrast and calm, mysterious mood.",
                "",
                "Style variation B:",
                "- Colorful modern illustration.",
                "- Multiple abstract elements related to the topic.",
                "- Energetic, playful quiz-game atmosphere.",
                "",
                "Additional style hint:",
                styleHint
        );
    }

    private String truncate(String text) {
        if (text == null) return null;
        String trimmed = text.trim();
        if (trimmed.length() <= 200) {
            return trimmed;
        }
        return trimmed.substring(0, 200) + "...";
    }

    private String buildEndpoint(String baseUrl, String model) {
        if (baseUrl == null || baseUrl.isBlank()) {
            return "";
        }
        String endpoint = baseUrl.endsWith("/") ? baseUrl : baseUrl + "/";
        return endpoint + model + ":generateContent";
    }
}
