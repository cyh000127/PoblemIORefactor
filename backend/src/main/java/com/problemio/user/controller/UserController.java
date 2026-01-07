package com.problemio.user.controller;

import com.problemio.global.auth.CustomUserDetails;
import com.problemio.global.common.ApiResponse;
import com.problemio.global.exception.BusinessException;
import com.problemio.global.exception.ErrorCode;
import com.problemio.quiz.dto.QuizSummaryDto;
import com.problemio.quiz.service.QuizService;
import com.problemio.user.dto.UserPopoverResponse;
import com.problemio.user.dto.UserResponse;
import com.problemio.user.dto.UserSummaryDto;
import com.problemio.global.service.S3Service;

import com.problemio.user.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.Map;


@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;
    private final QuizService quizService;
    private final S3Service s3Service;

    @Value("${file.upload-dir}")
    private String uploadDir;

    // 특정 유저 정보 조회
    @GetMapping("/{userId}")
    public ResponseEntity<ApiResponse<UserResponse>> getUser(
            @PathVariable Long userId,
            @AuthenticationPrincipal CustomUserDetails userDetails // 로그인 여부 확인
    ) {
        Long viewerId = (userDetails != null) ? userDetails.getUser().getId() : null;
        return ResponseEntity.ok(ApiResponse.success(userService.getUserProfile(userId, viewerId)));
    }

    // 내 정보 조회
    @GetMapping("/me")
    public ResponseEntity<ApiResponse<UserResponse>> getMe(@AuthenticationPrincipal CustomUserDetails userDetails) {
        return ResponseEntity.ok(ApiResponse.success(userService.getUserById(requireLogin(userDetails))));
    }

    // 프로필 수정
    @PatchMapping(value = "/me", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<ApiResponse<UserResponse>> updateProfile(
            @RequestPart(value = "data") UserResponse request,
            @RequestPart(value = "file", required = false) MultipartFile file, // [주의] 파라미터명 'file' 필수
            @AuthenticationPrincipal CustomUserDetails userDetails) {

        Long myId = requireLogin(userDetails);
        // 파일 수신 로그
        if (file != null && !file.isEmpty()) {
            System.out.println(">>> [Controller] 파일 수신 성공: " + file.getOriginalFilename());
        } else {
            System.out.println(">>> [Controller] 파일이 없거나 비어 있음");
        }

        return ResponseEntity.ok(ApiResponse.success(userService.updateProfile(myId, request, file)));
    }

    // 비밀번호 변경
    @PatchMapping("/me/password")
    public ResponseEntity<ApiResponse<Void>> changePassword(
            @RequestBody Map<String, String> request,
            @AuthenticationPrincipal CustomUserDetails userDetails) {
        Long userId = requireLogin(userDetails);
        userService.changePassword(userId, request.get("oldPassword"), request.get("newPassword"));
        return ResponseEntity.ok(ApiResponse.success(null));
    }

    // 회원 탈퇴 (비밀번호 확인 필요)
    @PostMapping("/me/withdraw")
    public ResponseEntity<ApiResponse<Void>> deleteAccount(
            @RequestBody Map<String, String> request,
            @AuthenticationPrincipal CustomUserDetails userDetails) {
        Long userId = requireLogin(userDetails);
        userService.deleteAccount(userId, request.get("password"));
        return ResponseEntity.ok(ApiResponse.success(null));
    }

    // 마이페이지 요약 정보 조회
    @GetMapping("/me/summary")
    public ResponseEntity<ApiResponse<UserSummaryDto>> getMySummary(@AuthenticationPrincipal CustomUserDetails userDetails) {
        Long userId = requireLogin(userDetails);
        return ResponseEntity.ok(ApiResponse.success(userService.getMySummary(userId)));
    }

    // 닉네임 중복 확인
    @GetMapping("/checkNickname")
    public ResponseEntity<ApiResponse<Boolean>> checkNickname(@RequestParam String nickname) {
        userService.checkNicknameDuplicate(nickname);
        // 예외 미발생 시 사용 가능
        return ResponseEntity.ok(ApiResponse.success(true));
    }

    // 팝오버 정보 조회
    @GetMapping("/{userId}/popover")
    public ResponseEntity<ApiResponse<UserPopoverResponse>> getUserPopover(
            @PathVariable Long userId,
            @AuthenticationPrincipal CustomUserDetails userDetails
    ) {
        // 1. 로그인 여부에 따른 뷰어 ID 설정
        Long viewerId = (userDetails != null) ? userDetails.getUser().getId() : null;

        // 2. 서비스 로직 수행
        UserPopoverResponse res = userService.getUserPopover(userId, viewerId);

        return ResponseEntity.ok(ApiResponse.success(res));
    }

    // 팔로우한 유저들의 퀴즈 목록 조회
    @GetMapping("/me/quizzes/followings")
    public ResponseEntity<ApiResponse<java.util.List<QuizSummaryDto>>> getQuizzesOfFollowings(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int size,
            @AuthenticationPrincipal CustomUserDetails userDetails
    ) {
        Long userId = requireLogin(userDetails);
        var data = quizService.getQuizzesOfFollowings(userId, page, size);
        return ResponseEntity.ok(ApiResponse.success(data));
    }

    // 좋아요한 퀴즈 목록 조회
    @GetMapping("/me/quizzes/liked")
    public ResponseEntity<ApiResponse<java.util.List<QuizSummaryDto>>> getLikedQuizzes(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int size,
            @AuthenticationPrincipal CustomUserDetails userDetails
    ) {
        Long userId = requireLogin(userDetails);
        var data = quizService.getLikedQuizzes(userId, page, size);
        return ResponseEntity.ok(ApiResponse.success(data));
    }

    // 특정 유저의 다른 퀴즈 목록 조회
    @GetMapping("/{userId}/quizzes")
    public ResponseEntity<ApiResponse<List<QuizSummaryDto>>> getUserQuizzes(
            @PathVariable Long userId
    ) {
        List<QuizSummaryDto> quizzes = quizService.getUserQuizzes(userId);
        return ResponseEntity.ok(ApiResponse.success(quizzes));
    }

    // 리소스(테마, 아바타, 팝오버) 목록 조회
    @GetMapping("/resources/{type}")
    public ResponseEntity<ApiResponse<List<String>>> getResources(@PathVariable String type) {
        // 리소스 타입 검증
        if (!List.of("theme", "avatar", "popover").contains(type)) {
            return ResponseEntity.badRequest().body(ApiResponse.fail("BAD_REQUEST", "Invalid resource type"));
        }

        // 로컬 디렉토리에서 prefix 기준 이미지 조회
        String prefix = "public/" + type.toLowerCase();
        List<String> fileNames = s3Service.listFiles(prefix).stream()
                   .filter(this::isImageFile)
                   .map(this::buildUrlFromKey)
                   .toList();

        return ResponseEntity.ok(ApiResponse.success(fileNames));
    }

    private String buildUrlFromKey(String key) {
        String cleanKey = key.startsWith("/") ? key.substring(1) : key;
        return "/uploads/" + cleanKey;
    }

    private boolean isImageFile(String filename) {
        String lower = filename.toLowerCase();
        return lower.endsWith(".jpg") || lower.endsWith(".jpeg") || 
               lower.endsWith(".png") || lower.endsWith(".gif") || 
               lower.endsWith(".webp") || lower.endsWith(".svg");
    }

    private Long requireLogin(CustomUserDetails userDetails) {
        if (userDetails == null) {
            throw new BusinessException(ErrorCode.LOGIN_REQUIRED);
        }
        return userDetails.getUser().getId();
    }
}
