package com.problemio.user.service;

import com.problemio.comment.mapper.CommentLikeMapper;
import com.problemio.comment.mapper.CommentMapper;
import com.problemio.follow.mapper.FollowMapper;
import com.problemio.global.exception.BusinessException;
import com.problemio.global.exception.ErrorCode;
import com.problemio.global.service.S3Service;
import com.problemio.quiz.domain.Quiz;
import com.problemio.quiz.dto.QuizSummaryDto;
import com.problemio.quiz.mapper.QuizLikeMapper;
import com.problemio.quiz.mapper.QuizMapper;
import com.problemio.quiz.service.QuizService;
import com.problemio.submission.mapper.SubmissionDetailMapper;
import com.problemio.submission.mapper.SubmissionMapper;
import com.problemio.user.domain.User;
import com.problemio.user.dto.UserPopoverResponse;
import com.problemio.user.dto.UserResponse;
import com.problemio.user.dto.UserSummaryDto;
import com.problemio.user.mapper.RefreshTokenMapper;
import com.problemio.user.mapper.UserAuthMapper;
import com.problemio.user.mapper.UserMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import com.problemio.global.util.TimeUtils;

@Service
@RequiredArgsConstructor
public class UserServiceImpl implements UserService {

    private final S3Service s3Service;

    // S3 경로 상수
    private static final String PROFILE_DIR = "public/upload/profile";

    private final UserMapper userMapper;
    private final UserAuthMapper userAuthMapper;
    private final PasswordEncoder passwordEncoder;
    private final QuizService quizService;
    private final QuizMapper quizMapper;
    private final QuizLikeMapper quizLikeMapper;
    private final CommentMapper commentMapper;
    private final CommentLikeMapper commentLikeMapper;
    private final FollowMapper followMapper;
    private final SubmissionMapper submissionMapper;
    private final SubmissionDetailMapper submissionDetailMapper;
    private final RefreshTokenMapper refreshTokenMapper;
    private final CacheManager cacheManager;

    @Override
    @Transactional(readOnly = true)
    public UserResponse getUserById(Long id) {
        return userMapper.findById(id)
                .orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND));
    }

    @Override
    @Transactional(readOnly = true)
    public UserResponse getUserProfile(Long userId, Long viewerId) {
        long vId = (viewerId == null) ? 0L : viewerId;
        UserResponse user = userMapper.findUserProfile(userId, vId);

        if (user == null) {
            throw new BusinessException(ErrorCode.USER_NOT_FOUND);
        }
        user.setProfileTheme(normalizeDecorationValue(user.getProfileTheme()));
        user.setAvatarDecoration(normalizeDecorationValue(user.getAvatarDecoration()));
        user.setPopoverDecoration(normalizeDecorationValue(user.getPopoverDecoration()));
        return user;
    }

    @Override
    @Transactional
    public UserResponse updateProfile(Long userId, UserResponse request, MultipartFile file) {
        UserResponse oldUser = getUserById(userId);
        
        // 금칙어 검사
        String nickname = request.getNickname();
        if (nickname != null
                && !nickname.equals(oldUser.getNickname())
                && (nickname.contains("admin") || nickname.contains("관리자") || nickname.contains("운영자"))) {
            throw new BusinessException(ErrorCode.INVALID_INPUT_VALUE);
        }

        String oldFilePath = oldUser.getProfileImageUrl();

        if (file != null && !file.isEmpty()) {
            
            // 기존 파일 삭제
            s3Service.delete(oldFilePath);

            // 새 파일 저장
            String originalFilename = file.getOriginalFilename();
            String extension = "";
            if (originalFilename != null && originalFilename.lastIndexOf('.') > -1) {
                extension = originalFilename.substring(originalFilename.lastIndexOf('.'));
            }
            String s3Key = PROFILE_DIR + "/" + UUID.randomUUID() + extension;
            
            String savedPath = s3Service.upload(file, s3Key);
            request.setProfileImageUrl(savedPath);
            
        } else {
            if (request.getProfileImageUrl() == null) {
                request.setProfileImageUrl(oldFilePath);
            }
        }

        if (request.getStatusMessage() != null && request.getStatusMessage().length() > 20) {
            throw new BusinessException(ErrorCode.INVALID_INPUT_VALUE);
        }

        // URL 형태의 설정값 정규화 (ID만 추출)
        request.setProfileTheme(normalizeDecorationValue(request.getProfileTheme()));
        request.setAvatarDecoration(normalizeDecorationValue(request.getAvatarDecoration()));
        request.setPopoverDecoration(normalizeDecorationValue(request.getPopoverDecoration()));

        request.setId(userId);
        request.setUpdatedAt(TimeUtils.now());
        userMapper.updateProfile(request);

        evictUserCaches(oldUser.getEmail(), userId);
        return getUserById(userId);
    }

    @Override
    @Transactional
    public void changePassword(Long userId, String oldPassword, String newPassword) {
        User user = userAuthMapper.findById(userId)
                .orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND));

        if (!passwordEncoder.matches(oldPassword, user.getPasswordHash())) {
            throw new BusinessException(ErrorCode.INVALID_LOGIN);
        }

        String encodedPassword = passwordEncoder.encode(newPassword);
        userMapper.updatePassword(userId, encodedPassword, TimeUtils.now());
        evictUserCaches(user.getEmail(), userId);
    }

    @Override
    @Transactional
    public void deleteAccount(Long userId, String password) {
        User user = userAuthMapper.findById(userId)
                .orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND));

        if (!passwordEncoder.matches(password, user.getPasswordHash())) {
            throw new BusinessException(ErrorCode.INVALID_LOGIN);
        }

        refreshTokenMapper.deleteByUserId(userId);
        followMapper.deleteByUserId(userId);

        List<Long> likedQuizIds = quizLikeMapper.findQuizIdsByUserId(userId);
        if (!likedQuizIds.isEmpty()) {
            quizLikeMapper.deleteByUserId(userId);
            likedQuizIds.forEach(quizMapper::decrementLikeCount);
        }

        List<Long> likedCommentIds = commentLikeMapper.findLikedCommentIdsByUser(userId);
        if (!likedCommentIds.isEmpty()) {
            commentLikeMapper.deleteByUserId(userId);
            likedCommentIds.forEach(commentMapper::decreaseLikeCount);
        }

        List<Long> myCommentIds = commentMapper.findIdsByUserId(userId);
        if (!myCommentIds.isEmpty()) {
            commentMapper.anonymizeByUserId(userId, TimeUtils.now());
        }

        List<Quiz> myQuizzes = quizMapper.findQuizzesByUserId(userId);
        for (Quiz quiz : myQuizzes) {
            quizService.deleteQuiz(userId, quiz.getId());
        }

        evictUserCaches(user.getEmail(), userId);

        String tombstone = "deleted_" + UUID.randomUUID();
        userMapper.anonymizeCredentials(userId, tombstone + "@deleted.local", tombstone, TimeUtils.now());
        userMapper.deleteUser(userId, TimeUtils.now());
    }

    @Override
    @Transactional(readOnly = true)
    public UserSummaryDto getMySummary(Long userId) {
        UserResponse user = getUserById(userId);
        return UserSummaryDto.builder()
                .userId(userId)
                .nickname(user.getNickname())
                .followerCount(userMapper.countFollowers(userId))
                .followingCount(userMapper.countFollowings(userId))
                .createdQuizCount(userMapper.countCreatedQuizzes(userId))
                .build();
    }

    @Override
    @Transactional(readOnly = true)
    public void checkNicknameDuplicate(String nickname) {
        if (userMapper.existsByNickname(nickname) > 0) {
            throw new BusinessException(ErrorCode.NICKNAME_DUPLICATED);
        }
    }

    @Override
    @Transactional(readOnly = true)
    public List<QuizSummaryDto> getMyQuizzes(Long userId) {
        return List.of();
    }

    @Override
    @Transactional(readOnly = true)
    public List<QuizSummaryDto> getMyLikedQuizzes(Long userId) {
        return List.of();
    }

    @Override
    @Transactional(readOnly = true)
    public List<QuizSummaryDto> getFollowingQuizzes(Long userId) {
        return List.of();
    }

    @Override
    @Transactional(readOnly = true)
    public UserPopoverResponse getUserPopover(Long userId, Long viewerId) {
        Long searchViewerId = (viewerId == null) ? 0L : viewerId;
        UserPopoverResponse res = userMapper.findUserPopover(userId, searchViewerId);

        if (res == null) {
            throw new BusinessException(ErrorCode.USER_NOT_FOUND);
        }

        res.setProfileTheme(normalizeDecorationValue(res.getProfileTheme()));
        res.setAvatarDecoration(normalizeDecorationValue(res.getAvatarDecoration()));
        res.setPopoverDecoration(normalizeDecorationValue(res.getPopoverDecoration()));

        if (viewerId == null) {
            res.setMe(false);
        } else {
            res.setMe(userId.equals(viewerId));
        }

        return res;
    }

    /**
     * 인증 캐시(UserDetails) 무효화 (이메일 및 ID 기준)
     */
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

    /**
     * 프로필 설정값 정규화: URL에서 파일명(ID) 추출
     * 이미 ID 형태인 경우 그대로 반환
     */
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
