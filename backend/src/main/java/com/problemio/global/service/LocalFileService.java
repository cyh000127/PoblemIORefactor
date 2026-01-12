package com.problemio.global.service;

import com.problemio.global.exception.BusinessException;
import com.problemio.global.exception.ErrorCode;

import jakarta.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;

@Service
public class LocalFileService {

    @Value("${file.upload-dir}")
    private String uploadDir;

    private Path fileStorageLocation;

    @PostConstruct
    public void init() {
        try {
            this.fileStorageLocation = Paths.get(uploadDir).toAbsolutePath().normalize();
            Files.createDirectories(this.fileStorageLocation);
        } catch (Exception ex) {
            throw new BusinessException(ErrorCode.INTERNAL_SERVER_ERROR);
        }
    }

    /**
     * 파일을 로컬에 저장하고 상대 경로(Key)를 반환합니다.
     * key는 호출자가 지정한 경로(폴더+파일명)를 그대로 사용합니다.
     */
    public String upload(MultipartFile file, String fileKey) {
        try {
            // fileKey에 포함된 디렉토리 경로 생성
            Path targetPath = this.fileStorageLocation.resolve(fileKey).normalize();
            Files.createDirectories(targetPath.getParent());

            Files.copy(file.getInputStream(), targetPath, StandardCopyOption.REPLACE_EXISTING);

            return fileKey;

        } catch (IOException e) {
            throw new BusinessException(ErrorCode.INTERNAL_SERVER_ERROR);
        }
    }

    /**
     * 바이트 배열을 로컬에 저장하고 Key를 반환합니다.
     */
    public String uploadBytes(byte[] bytes, String fileKey, String contentType) {
        try {
            Path targetPath = this.fileStorageLocation.resolve(fileKey).normalize();
            Files.createDirectories(targetPath.getParent());

            Files.write(targetPath, bytes);
            return fileKey;
        } catch (Exception e) {
            throw new BusinessException(ErrorCode.INTERNAL_SERVER_ERROR);
        }
    }

    /**
     * 파일을 삭제합니다.
     * 파라미터로 전체 URL이 들어오더라도 Key를 추출하여 삭제합니다.
     */
    public void delete(String fileUrl) {
        if (fileUrl == null || fileUrl.isBlank()) return;

        try {
            String key = extractKeyFromUrl(fileUrl);
            if (key != null) {
                Path filePath = this.fileStorageLocation.resolve(key).normalize();
                Files.deleteIfExists(filePath);
            }
        } catch (Exception e) {
            System.err.println("File Delete Error: " + e.getMessage());
        }
    }

    /**
     * 지정된 prefix(디렉토리 경로)의 파일 목록을 반환합니다.
     * 반환값은 Key(상대 경로) 리스트입니다.
     */
    public java.util.List<String> listFiles(String prefix) {
        try {
            Path dir = this.fileStorageLocation.resolve(prefix).normalize();
            if (!Files.exists(dir) || !Files.isDirectory(dir)) {
                return java.util.Collections.emptyList();
            }

            try (java.util.stream.Stream<Path> stream = Files.walk(dir, 1)) {
                return stream
                        .filter(file -> !Files.isDirectory(file))
                        .map(file -> {
                            // 절대 경로에서 uploadDir 부분 제거하고 forward slash로 변환
                            String relative = this.fileStorageLocation.relativize(file).toString();
                            return relative.replace("\\", "/");
                        })
                        .toList();
            }
        } catch (IOException e) {
            return java.util.Collections.emptyList();
        }
    }

    private String extractKeyFromUrl(String fileUrl) {
        if (fileUrl == null) return null;
        // /uploads/ 접두어 제거하고 키 추출
        if (fileUrl.contains("/uploads/")) {
            return fileUrl.substring(fileUrl.indexOf("/uploads/") + 9);
        }
        return fileUrl;
    }

    public String getUploadDir() {
        return uploadDir;
    }
}
