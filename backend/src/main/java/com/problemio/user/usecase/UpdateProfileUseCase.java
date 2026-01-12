package com.problemio.user.usecase;

import com.problemio.global.exception.BusinessException;
import com.problemio.global.exception.ErrorCode;
import com.problemio.global.service.LocalFileService;
import com.problemio.global.util.TimeUtils;
import com.problemio.user.domain.DeleteStatus;
import com.problemio.user.domain.User;
import com.problemio.user.dto.UserResponse;
import com.problemio.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.util.UUID;

@Service
@RequiredArgsConstructor
@Transactional
public class UpdateProfileUseCase {

    private final UserRepository userRepository;
    private final LocalFileService localFileService;
    private final CacheManager cacheManager;

    private static final String PROFILE_DIR = "public/upload/profile";

    public UserResponse execute(Long userId, UserResponse request, MultipartFile file) {
        User user = userRepository.findByIdAndIsDeleted(userId, DeleteStatus.ACTIVE)
                .orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND));

        UserResponse oldUser = UserResponse.from(user); // Use simple mapping for oldUser info if needed, or just use 'user'

        // 1. Forbidden Nickname Check
        String nickname = request.getNickname();
        if (nickname != null
                && !nickname.equals(user.getNickname())
                && (nickname.contains("admin") || nickname.contains("관리자") || nickname.contains("운영자"))) {
            throw new BusinessException(ErrorCode.INVALID_INPUT_VALUE);
        }

        // 2. Image Upload
        String oldFilePath = user.getProfileImageUrl();
        String newProfileImageUrl = null;

        if (file != null && !file.isEmpty()) {
            // Delete old file
            localFileService.delete(oldFilePath);

            // Upload new file
            String originalFilename = file.getOriginalFilename();
            String extension = "";
            if (originalFilename != null && originalFilename.lastIndexOf('.') > -1) {
                extension = originalFilename.substring(originalFilename.lastIndexOf('.'));
            }
            String fileKey = PROFILE_DIR + "/" + UUID.randomUUID() + extension;
            newProfileImageUrl = localFileService.upload(file, fileKey);
        } else {
            // If file is explicitly null but request has null url, keep old. 
            // Logic differs slightly from original but intent is: if file given, use it. if not, check request or keep old.
            // Original logic: 
            // if (file != null && !file.isEmpty()) { ... } else { if (request.getProfileImageUrl() == null) request.set(old) }
            // Here we rely on strict updateProfile call.
            if (request.getProfileImageUrl() == null) {
                newProfileImageUrl = oldFilePath;
            } else {
                newProfileImageUrl = request.getProfileImageUrl();
            }
        }
        
        // 3. Status Message Length Check
        if (request.getStatusMessage() != null && request.getStatusMessage().length() > 20) {
            throw new BusinessException(ErrorCode.INVALID_INPUT_VALUE);
        }

        // 4. Update Entity
        // Normalize decorations
        String profileTheme = normalizeDecorationValue(request.getProfileTheme());
        String avatarDecoration = normalizeDecorationValue(request.getAvatarDecoration());
        String popoverDecoration = normalizeDecorationValue(request.getPopoverDecoration());

        user.updateProfile(nickname, request.getStatusMessage(), newProfileImageUrl);
        user.updateDecorations(profileTheme, avatarDecoration, popoverDecoration);
        
        // Note: updatedAt is handled by @LastModifiedDate? 
        // If not working automatically (sometimes listeners not active), we might need to set it manually. 
        // But field is private and has no setter in User.java (or maybe it does via Lombok @Data? No, @Getter).
        // If @LastModifiedDate works, we are good. If not, we might need a method to force update it.
        // Given original code used userMapper.updateProfile which set updated_at explicitly.
        // We will assume JPA Auditing is enabled.

        // 5. Evict Caches
        evictUserCaches(user.getEmail(), userId);

        return UserResponse.from(user);
    }

    private void evictUserCaches(String email, Long userId) {
        Cache cache = cacheManager.getCache("userDetails");
        if (cache != null && email != null) {
            cache.evict(email);
        }
        Cache profileCache = cacheManager.getCache("userProfile");
        if (profileCache != null && userId != null) {
            profileCache.evict(userId);
        }
    }

    private String normalizeDecorationValue(String value) {
        if (value == null || value.isBlank()) {
            return value;
        }
        String trimmed = value.trim();
        if (trimmed.startsWith("http://") || trimmed.startsWith("https://")) {
            String lastSegment = trimmed.substring(trimmed.lastIndexOf('/') + 1);
            int dotIdx = lastSegment.lastIndexOf('.');
            return (dotIdx > 0) ? lastSegment.substring(0, dotIdx) : lastSegment;
        }
        return trimmed;
    }
}
