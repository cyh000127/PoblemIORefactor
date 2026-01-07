package com.problemio.admin.controller;

import com.problemio.admin.service.AdminService;
import com.problemio.challenge.dto.ChallengeCreateRequest;
import com.problemio.global.auth.CustomUserDetails;
import com.problemio.quiz.domain.Quiz;
import com.problemio.quiz.dto.QuizResponse;
import com.problemio.global.dto.PageResponse;
import com.problemio.user.dto.UserResponse;
import com.problemio.user.mapper.UserMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import jakarta.validation.Valid;
import java.util.List;
import java.util.stream.Collectors;
import org.springframework.beans.factory.annotation.Autowired;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

@RestController
@RequestMapping("/api/admin")
@RequiredArgsConstructor
public class AdminController {

    private final AdminService adminService;
    private final UserMapper userMapper; // 응답용 작성자 정보 fetch

    // 관리자 권한 확인 (SecurityConfig 또는 @PreAuthorize 처리 필요)
    
    @GetMapping("/quizzes")
    public PageResponse<QuizResponse> getAdminQuizzes(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(required = false) String keyword
    ) {
        List<Quiz> quizzes = adminService.findAdminQuizzes(page, size, keyword);
        int totalElements = adminService.countAdminQuizzes(keyword);
        int totalPages = (int) Math.ceil((double) totalElements / size);
        
        // Convert to QuizResponse
        // Admin 뷰를 위한 기본 정보 및 숨김 상태 조회 (효율성을 위해 좋아요/팔로우 체크 제외)
        
        List<QuizResponse> content = quizzes.stream().map(quiz -> {
            // 작성자 정보 매핑
             UserResponse author = userMapper.findById(quiz.getUserId())
                     .map(u -> UserResponse.builder()
                             .id(u.getId())
                             .nickname(u.getNickname())
                             .email(u.getEmail())
                             .profileImageUrl(u.getProfileImageUrl())
                             .build())
                     .orElse(null);

            return QuizResponse.builder()
                    .id(quiz.getId())
                    .userId(quiz.getUserId())
                    .title(quiz.getTitle())
                    .description(quiz.getDescription())
                    .thumbnailUrl(quiz.getThumbnailUrl())
                    .isPublic(quiz.isPublic())
                    .isHidden(quiz.isHidden())
                    .likeCount(quiz.getLikeCount())
                    .playCount(quiz.getPlayCount())
                    .author(author)
                    .build();
        }).collect(Collectors.toList());

        return PageResponse.<QuizResponse>builder()
                .content(content)
                .totalElements(totalElements)
                .totalPages(totalPages)
                .size(size)
                .page(page)
                .build();
    }

    @PatchMapping("/quizzes/{id}/hide")
    public ResponseEntity<Void> toggleQuizVisibility(@PathVariable Long id) {
        adminService.toggleQuizVisibility(id);
        return ResponseEntity.ok().build();
    }

    @PostMapping("/challenges")
    public ResponseEntity<Void> createChallenge(@Valid @RequestBody ChallengeCreateRequest request) {
        adminService.createChallenge(request);
        return ResponseEntity.status(HttpStatus.CREATED).build();
    }

    // --- 커스텀 아이템 관리 ---

    @Autowired
    private com.problemio.item.service.CustomItemService customItemService;

    @PostMapping("/items")
    public ResponseEntity<Void> createCustomItem(@RequestBody com.problemio.item.dto.CustomItemRequest request) {
        customItemService.createItem(request);
        return ResponseEntity.status(HttpStatus.CREATED).build();
    }

    @GetMapping("/items")
    public ResponseEntity<List<com.problemio.item.domain.CustomItem>> getAllCustomItems() {
        return ResponseEntity.ok(customItemService.getAllItems());
    }

    @PostMapping("/items/{itemId}/assign/{userId}")
    public ResponseEntity<Void> assignItemToUser(@PathVariable Long itemId, @PathVariable Long userId) {
        customItemService.assignItemToUser(itemId, userId);
        return ResponseEntity.ok().build();
    }

    @PutMapping("/items/{itemId}")
    public ResponseEntity<Void> updateCustomItem(@PathVariable Long itemId, @RequestBody com.problemio.item.dto.CustomItemRequest request) {
        customItemService.updateItem(itemId, request);
        return ResponseEntity.ok().build();
    }

    @GetMapping("/items/{itemId}/users")
    public ResponseEntity<List<com.problemio.user.dto.UserResponse>> getAssignedUsers(@PathVariable Long itemId) {
        return ResponseEntity.ok(customItemService.getAssignedUsers(itemId));
    }

    @DeleteMapping("/items/{itemId}/users/{userId}")
    public ResponseEntity<Void> revokeUserItem(@PathVariable Long itemId, @PathVariable Long userId) {
        customItemService.revokeUserItem(itemId, userId);
        return ResponseEntity.ok().build();
    }

    // 아이템 삭제
    @DeleteMapping("/items/{itemId}")
    public ResponseEntity<Void> deleteCustomItem(@PathVariable Long itemId) {
        customItemService.deleteItem(itemId);
        return ResponseEntity.ok().build();
    }
    @Autowired
    private com.problemio.global.service.S3Service s3Service;

    @PostMapping("/items/upload")
    public ResponseEntity<String> uploadItemImage(@RequestParam("file") org.springframework.web.multipart.MultipartFile file, @RequestParam("type") String type) {
        if (file.isEmpty()) {
            return ResponseEntity.badRequest().body("File is empty");
        }

        try {
            // 타입별 디렉토리 결정
            String subDir = "theme";
            if ("POPOVER".equalsIgnoreCase(type)) subDir = "popover";
            else if ("AVATAR".equalsIgnoreCase(type)) subDir = "avatar";
            
            // 형식: public/{subDir}/{timestamp}_{originalName}
            String originalFilename = org.springframework.util.StringUtils.cleanPath(file.getOriginalFilename());
            String filename = System.currentTimeMillis() + "_" + originalFilename;
            String s3Key = "public/" + subDir + "/" + filename;
            
            // S3 업로드
            String uploadedPath = s3Service.upload(file, s3Key);
            
            // DB 저장용 경로(s3Key) 반환 (프론트엔드에서 Base URL 연결)
            return ResponseEntity.ok(uploadedPath);

        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.internalServerError().body("Failed to upload file: " + e.getMessage());
        }
    }
}
