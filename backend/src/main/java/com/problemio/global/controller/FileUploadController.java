package com.problemio.global.controller;

import com.problemio.global.auth.CustomUserDetails;
import com.problemio.global.common.ApiResponse;
import com.problemio.global.exception.BusinessException;
import com.problemio.global.exception.ErrorCode;
import com.problemio.global.service.LocalFileService;
import lombok.RequiredArgsConstructor;
import org.apache.tika.Tika;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/files")
@RequiredArgsConstructor
public class FileUploadController {

    private final LocalFileService localFileService;

    // 파일 저장될 하위 디렉토리 명 (요청된 구조 반영)
    // upload 하위
    private static final String DIR_UPLOAD_THUMBNAIL = "public/upload/questions"; // 문제 이미지를 questions에 저장
    // 주석: "questions # 퀘스천 사진 저장", "thumbnail # 문제 썸네일 저장"
    // 변수명 매칭 수정
    private static final String DIR_UPLOAD_QUESTIONS = "public/upload/questions";
    private static final String DIR_UPLOAD_THUMBNAIL_REAL = "public/upload/thumbnail";
    
    // public 하위
    private static final String DIR_POPOVER = "public/popover";
    private static final String DIR_THEME = "public/theme";
    private static final String DIR_AVATAR = "public/avatar";

    private final Tika tika = new Tika();
    private static final List<String> ALLOWED_EXTENSIONS = Arrays.asList(".jpg", ".jpeg", ".png", ".gif", ".webp");
    private static final List<String> ALLOWED_MIME_TYPES = Arrays.asList("image/jpeg", "image/png", "image/gif", "image/webp");

    @PostMapping("/upload")
    public ResponseEntity<ApiResponse<Map<String, String>>> upload(
            @RequestParam("file") MultipartFile file,
            @RequestParam(value = "category", required = false) String category,
            @AuthenticationPrincipal CustomUserDetails userDetails
    ) {

        if (userDetails == null) {
            throw new BusinessException(ErrorCode.LOGIN_REQUIRED);
        }

        if (file.isEmpty()) {
            return ResponseEntity.badRequest()
                    .body(ApiResponse.fail("FILE_UPLOAD_ERROR", "파일이 비어있습니다."));
        }

        validateFile(file);

        String originalFilename = StringUtils.cleanPath(file.getOriginalFilename());
        String extension = "";
        int dotIndex = originalFilename.lastIndexOf('.');
        if (dotIndex > -1) {
            extension = originalFilename.substring(dotIndex);
        }

        String filename = UUID.randomUUID() + extension;
        String subDir = resolveSubDirectory(category);
        
        // File Key 생성
        String fileKeyPath = subDir + "/" + filename;

        // 서비스 호출 (업로드 후 Key 반환)
        String fileKey = localFileService.upload(file, fileKeyPath);

        // 로컬 URL 생성
        String fullUrl = "/uploads/" + (fileKey.startsWith("/") ? fileKey.substring(1) : fileKey);

        return ResponseEntity.ok(ApiResponse.success(Map.of(
                "url", fullUrl,
                "filename", filename
        )));
    }

    private String resolveSubDirectory(String category) {
        if (category == null || category.isBlank()) {
            return "public/upload/misc";
        }

        String normalized = category.trim().toLowerCase();
        return switch (normalized) {
            case "thumbnail", "thumbnails" -> DIR_UPLOAD_THUMBNAIL_REAL;
            case "question", "questions" -> DIR_UPLOAD_QUESTIONS;
            case "popover" -> DIR_POPOVER;
            case "theme" -> DIR_THEME;
            case "avatar" -> DIR_AVATAR;
            default -> "public/upload/misc";
        };
    }

    private void validateFile(MultipartFile file) {
        String originalFilename = file.getOriginalFilename();
        if (originalFilename == null || originalFilename.isBlank()) {
            throw new BusinessException(ErrorCode.INVALID_FILE_TYPE);
        }

        String lowerCaseName = originalFilename.toLowerCase();
        boolean validExtension = ALLOWED_EXTENSIONS.stream().anyMatch(lowerCaseName::endsWith);
        if (!validExtension) {
            throw new BusinessException(ErrorCode.INVALID_FILE_TYPE);
        }

        try {
            String mimeType = tika.detect(file.getInputStream());
            if (!ALLOWED_MIME_TYPES.contains(mimeType)) {
                throw new BusinessException(ErrorCode.INVALID_FILE_TYPE);
            }
        } catch (IOException e) {
            throw new BusinessException(ErrorCode.INTERNAL_SERVER_ERROR);
        }
    }
}
