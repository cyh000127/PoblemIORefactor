package com.problemio.admin.service;

import com.problemio.challenge.domain.Challenge;
import com.problemio.challenge.dto.ChallengeCreateRequest;
import com.problemio.challenge.mapper.ChallengeMapper;
import com.problemio.global.exception.BusinessException;
import com.problemio.global.exception.ErrorCode;
import com.problemio.global.util.TimeUtils;
import com.problemio.quiz.domain.Quiz;
import com.problemio.quiz.mapper.QuizMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class AdminService {

    private final QuizMapper quizMapper;
    private final ChallengeMapper challengeMapper;

    @Transactional(readOnly = true)
    public List<Quiz> findAdminQuizzes(int page, int size, String keyword) {
        int offset = (Math.max(page, 1) - 1) * size;
        return quizMapper.findAdminQuizzes(offset, size, keyword);
    }

    @Transactional(readOnly = true)
    public int countAdminQuizzes(String keyword) {
        return quizMapper.countAdminQuizzes(keyword);
    }

    @Transactional
    public void toggleQuizVisibility(Long quizId) {
        Quiz quiz = quizMapper.findById(quizId)
                .orElseThrow(() -> new BusinessException(ErrorCode.QUIZ_NOT_FOUND));
        
        // 숨김 상태 토글
        quiz.setHidden(!quiz.isHidden());
        
        // 퀴즈 정보 업데이트
        quizMapper.updateQuiz(quiz);
    }

    @Transactional
    public void createChallenge(ChallengeCreateRequest request) {
        // 유효성 검사: 퀴즈 존재 여부
        if (quizMapper.findById(request.getTargetQuizId()).isEmpty()) {
            throw new BusinessException(ErrorCode.QUIZ_NOT_FOUND);
        }

        Challenge challenge = request.toEntity();
        LocalDateTime now = TimeUtils.now();
        challenge.setCreatedAt(now);
        challenge.setUpdatedAt(now);
        challengeMapper.insert(challenge);
    }
}
