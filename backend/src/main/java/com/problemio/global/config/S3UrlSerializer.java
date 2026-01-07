package com.problemio.global.config;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.SerializerProvider;


import java.io.IOException;

public class S3UrlSerializer extends JsonSerializer<String> {

    private static String s3Url = "/uploads/";

    public static void setS3Url(String url) {
        if (url != null && !url.isBlank()) {
            if (!url.endsWith("/")) {
                S3UrlSerializer.s3Url = url + "/";
            } else {
                S3UrlSerializer.s3Url = url;
            }
        }
    }

    @Override
    public void serialize(String value, JsonGenerator gen, SerializerProvider serializers) throws IOException {
        if (value == null) {
            gen.writeNull();
            return;
        }

        // 레거시 로컬 URL(/uploads/...)이면 S3 키 형태로 정규화 시도
        String normalized = normalizeLegacyUrl(value);

        // 이미 http로 시작하면 그대로 씀 (정규화 이후에도 http일 수 있음)
        if (normalized.startsWith("http://") || normalized.startsWith("https://")) {
            gen.writeString(normalized);
            return;
        }

        // DB에 저장된 상대 경로(public/...) 앞에 도메인 붙이기
        // S3_URL이 설정 안되어있으면 그냥 값 그대로 (로컬 테스트 등)
        if (s3Url != null) {
            // value가 /로 시작하면 제거 (중복 슬래시 방지)
            String path = normalized.startsWith("/") ? normalized.substring(1) : normalized;
            gen.writeString(s3Url + path);
        } else {
            gen.writeString(normalized);
        }
    }

    /**
     * 예전 로컬 절대경로(http://localhost:8080/uploads/...)를 S3 키(public/upload/...)로 치환합니다.
     * 치환 불가하면 원본을 반환합니다.
     */
    private String normalizeLegacyUrl(String value) {
        String lower = value.toLowerCase();

        // 1) 호스트 포함 로컬 업로드 URL
        if (lower.startsWith("http://localhost:8080/uploads/") || lower.startsWith("https://localhost:8080/uploads/")) {
            int idx = lower.indexOf("/uploads/");
            String path = value.substring(idx + "/uploads/".length());
            return remapUploadsPath(path);
        }

        // 2) 슬래시로 시작하는 로컬 업로드 경로
        if (lower.startsWith("/uploads/")) {
            String path = value.substring("/uploads/".length());
            return remapUploadsPath(path);
        }

        return value;
    }

    /**
     * /uploads 하위 경로를 S3 키 규칙으로 변환합니다.
     */
    private String remapUploadsPath(String path) {
        String clean = path.startsWith("/") ? path.substring(1) : path;
        String lowerClean = clean.toLowerCase();

        // 대표 케이스: Questions -> public/upload/questions
        String questionsPrefix = "questions/";
        if (lowerClean.startsWith(questionsPrefix)) {
            return "public/upload/questions/" + clean.substring(questionsPrefix.length());
        }

        // 그 외 uploads/* 은 일괄적으로 public/upload/* 로 매핑
        return "public/upload/" + clean;
    }
}
