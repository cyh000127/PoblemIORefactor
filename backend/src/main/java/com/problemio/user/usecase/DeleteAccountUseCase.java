package com.problemio.user.usecase;

import com.problemio.comment.mapper.CommentLikeMapper;
import com.problemio.comment.mapper.CommentMapper;
import com.problemio.follow.mapper.FollowMapper;
import com.problemio.global.exception.BusinessException;
import com.problemio.global.exception.ErrorCode;
import com.problemio.global.util.TimeUtils;
import com.problemio.quiz.domain.Quiz;
import com.problemio.quiz.mapper.QuizLikeMapper;
import com.problemio.quiz.mapper.QuizMapper;
import com.problemio.quiz.service.QuizService;
import com.problemio.user.domain.DeleteStatus;
import com.problemio.user.domain.User;
import com.problemio.user.mapper.RefreshTokenMapper;
import com.problemio.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Transactional
public class DeleteAccountUseCase {

    private final UserRepository userRepository;
    private final RefreshTokenMapper refreshTokenMapper;
    private final FollowMapper followMapper;
    private final QuizLikeMapper quizLikeMapper;
    private final QuizMapper quizMapper;
    private final CommentLikeMapper commentLikeMapper;
    private final CommentMapper commentMapper;
    private final QuizService quizService;
    private final PasswordEncoder passwordEncoder;
    private final CacheManager cacheManager;

    public void execute(Long userId, String password) {
        User user = userRepository.findByIdAndIsDeleted(userId, DeleteStatus.ACTIVE)
                .orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND));

        if (!passwordEncoder.matches(password, user.getPasswordHash())) {
            throw new BusinessException(ErrorCode.INVALID_LOGIN);
        }

        // 1. 연관 데이터 삭제 (기존 Mapper 사용하여 기능 유지)
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

        // 2. 캐시 무효화
        evictUserCaches(user.getEmail(), userId);

        // 3. 사용자 익명화 및 삭제 처리
        String tombstone = "deleted_" + UUID.randomUUID();
        user.anonymize(tombstone + "@deleted.local", tombstone);
        user.delete();
        
        // 참고: JPA Dirty checking으로 인해 업데이트 쿼리가 자동 실행됩니다.
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
}
