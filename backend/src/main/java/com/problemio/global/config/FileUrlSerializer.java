package com.problemio.global.config;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.SerializerProvider;

import java.io.IOException;

public class FileUrlSerializer extends JsonSerializer<String> {

    // 로컬 파일 서빙을 위한 기본 URL prefix
    // WebMvcConfig에서 /uploads/** -> file:///C:/public/upload/ 매핑됨
    private static String baseUrl = "/uploads/";

    public static void setBaseUrl(String url) {
        if (url != null && !url.isBlank()) {
            if (!url.endsWith("/")) {
                FileUrlSerializer.baseUrl = url + "/";
            } else {
                FileUrlSerializer.baseUrl = url;
            }
        }
    }

    @Override
    public void serialize(String value, JsonGenerator gen, SerializerProvider serializers) throws IOException {
        if (value == null) {
            gen.writeNull();
            return;
        }

        // 1) 이미 http(s)로 시작하는 완전한 URL이면 그대로 반환 (외부 링크 등)
        if (value.toLowerCase().startsWith("http://") || value.toLowerCase().startsWith("https://")) {
            gen.writeString(value);
            return;
        }

        // 2) 레거시 로컬 URL 정규화 (필요한 경우)
        String normalized = normalizeLegacyUrl(value);
        if (normalized.toLowerCase().startsWith("http://") || normalized.toLowerCase().startsWith("https://")) {
            gen.writeString(normalized);
            return;
        }

        // 3) 상대 경로(Key)인 경우 baseUrl(/uploads/)을 붙여서 반환
        // value가 /로 시작하면 제거
        String path = normalized.startsWith("/") ? normalized.substring(1) : normalized;
        gen.writeString(baseUrl + path);
    }

    /**
     * 예전 로컬 절대경로(http://localhost:8080/uploads/...) 등을 정리합니다.
     */
    private String normalizeLegacyUrl(String value) {
        String lower = value.toLowerCase();

        // 1) 호스트 포함 로컬 업로드 URL -> 상대 경로로 변환
        if (lower.startsWith("http://localhost:8080/uploads/") || lower.startsWith("https://localhost:8080/uploads/")) {
            int idx = lower.indexOf("/uploads/");
            return value.substring(idx + "/uploads/".length());
        }

        // 2) /uploads/ 로 시작하는 경우 -> 상대 경로로 변환
        if (lower.startsWith("/uploads/")) {
            return value.substring("/uploads/".length());
        }

        return value;
    }
}
