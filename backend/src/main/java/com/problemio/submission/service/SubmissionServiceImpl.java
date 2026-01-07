package com.problemio.submission.service;

import com.problemio.global.exception.BusinessException;
import com.problemio.global.exception.ErrorCode;
import com.problemio.quiz.domain.Question;
import com.problemio.quiz.domain.QuestionAnswer;
import com.problemio.quiz.mapper.QuestionAnswerMapper;
import com.problemio.quiz.mapper.QuestionMapper;
import com.problemio.quiz.mapper.QuizMapper;
import com.problemio.submission.domain.Submission;
import com.problemio.submission.domain.SubmissionDetail;
import com.problemio.submission.dto.QuizAnswerResponse;
import com.problemio.submission.dto.QuizPlayContextResponse;
import com.problemio.submission.dto.QuizSubmissionRequest;
import com.problemio.submission.dto.QuizSubmissionResponse;
import com.problemio.submission.mapper.SubmissionDetailMapper;
import com.problemio.submission.mapper.SubmissionMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Locale;

import com.problemio.global.util.TimeUtils;

@Service
@RequiredArgsConstructor
public class SubmissionServiceImpl implements SubmissionService {

    private final SubmissionMapper submissionMapper;
    private final SubmissionDetailMapper submissionDetailMapper;
    private final QuizMapper quizMapper;
    private final QuestionMapper questionMapper;
    private final QuestionAnswerMapper questionAnswerMapper;
    private final CacheManager cacheManager;

    @Override
    @Transactional(readOnly = true)
    public QuizPlayContextResponse getPlayContext(Long quizId) {
        var quiz = quizMapper.findById(quizId)
                .orElseThrow(() -> new BusinessException(ErrorCode.QUIZ_NOT_FOUND));

        List<Question> questions = questionMapper.findByQuizId(quizId);

        // 미리 캐시에 로드
        preloadQuestionCaches(questions, quizId);

        List<QuizPlayContextResponse.QuestionDto> questionDtos = questions.stream()
                .map(q -> QuizPlayContextResponse.QuestionDto.builder()
                        .id(q.getId())
                        .order(q.getQuestionOrder())
                        .imageUrl(q.getImageUrl())
                        .description(q.getDescription())
                        .build())
                .toList();

        return QuizPlayContextResponse.builder()
                .quizId(quizId)
                .title(quiz.getTitle())
                .description(quiz.getDescription())
                .thumbnailUrl(quiz.getThumbnailUrl())
                .totalQuestions(questions.size())
                .questions(questionDtos)
                .build();
    }

    @Override
    @Transactional
    public QuizAnswerResponse submitQuiz(Long quizId, Long userIdOrNull, QuizSubmissionRequest request) {
        // 퀴즈 존재 여부 확인
        quizMapper.findById(quizId).orElseThrow(() -> new BusinessException(ErrorCode.QUIZ_NOT_FOUND));

        // 문제 검증 및 정답 목록 조회
        Question question = getQuestionCached(request.getQuestionId());
        if (!question.getQuizId().equals(quizId)) {
            throw new BusinessException(ErrorCode.ACCESS_DENIED);
        }

        List<QuestionAnswer> answers = getQuestionAnswersCached(request.getQuestionId());
        boolean correct = isAnswerCorrect(request.getAnswerText(), answers);

        Submission submission = resolveSubmission(quizId, userIdOrNull, request.getSubmissionId(), request.getTotalQuestions());

        SubmissionDetail detail = new SubmissionDetail();
        detail.setSubmissionId(submission.getId());
        detail.setQuestionId(question.getId());
        detail.setCorrect(correct);
        submissionDetailMapper.insertSubmissionDetail(detail);

        int correctCount = submissionDetailMapper.countCorrectBySubmissionId(submission.getId());
        
        // Java에서 플레이 시간 업데이트
        LocalDateTime now = TimeUtils.now();
        double playTime = 0.0;
        if (submission.getSubmittedAt() != null) {
            java.time.Duration duration = java.time.Duration.between(submission.getSubmittedAt(), now);
            playTime = duration.getSeconds() + (duration.getNano() / 1_000_000_000.0);
        }
        
        submissionMapper.updatePlayTime(submission.getId(), quizId, playTime);
        submissionMapper.updateCorrectCount(submission.getId(), quizId, correctCount);

        int answeredCount = submissionDetailMapper.countBySubmissionId(submission.getId());
        submission.setCorrectCount(correctCount);

        return QuizAnswerResponse.builder()
                .submissionId(submission.getId())
                .questionId(question.getId())
                .correct(correct)
                .correctAnswer(answers.isEmpty() ? null : answers.get(0).getAnswerText())
                .correctAnswers(answers.stream().map(QuestionAnswer::getAnswerText).toList())
                .imageUrl(question.getImageUrl())
                .totalQuestions(submission.getTotalQuestions())
                .answeredCount(answeredCount)
                .correctCount(correctCount)
                .build();
    }

    @Override
    @Transactional(readOnly = true)
    public QuizSubmissionResponse getSubmissionResult(Long submissionId) {
        Submission submission = submissionMapper.findById(submissionId)
                .orElseThrow(() -> new BusinessException(ErrorCode.ACCESS_DENIED));

        int answeredCount = submissionDetailMapper.countBySubmissionId(submissionId);

        return QuizSubmissionResponse.builder()
                .submissionId(submission.getId())
                .totalQuestions(submission.getTotalQuestions())
                .correctCount(submission.getCorrectCount())
                .answeredCount(answeredCount)
                .build();
    }

    private Submission resolveSubmission(Long quizId, Long userIdOrNull, Long submissionId, Integer requestedTotal) {
        if (submissionId == null) {
            int totalQuestions = requestedTotal != null
                    ? requestedTotal
                    : questionMapper.findByQuizId(quizId).size();

            Submission submission = new Submission();
            submission.setQuizId(quizId);
            submission.setUserId(userIdOrNull);
            submission.setTotalQuestions(totalQuestions);
            submission.setCorrectCount(0);
            submission.setSubmittedAt(TimeUtils.now());
            submissionMapper.insertSubmission(submission);
            return submission;
        }

        // 기존 제출 이력 조회 (제출 시간 확인용)
        Submission submission = submissionMapper.findById(submissionId)
                .orElseThrow(() -> new BusinessException(ErrorCode.ACCESS_DENIED));
        
        // 필요 시 총 문항 수 업데이트?
        if (requestedTotal != null && !requestedTotal.equals(submission.getTotalQuestions())) {
            // submission.setTotalQuestions(requestedTotal); // Not persisting this update for now as it's not critical
        }
        
        return submission;
    }

    private boolean isAnswerCorrect(String userAnswer, List<QuestionAnswer> answers) {
        String normalizedUser = normalize(userAnswer);
        return answers.stream()
                .map(QuestionAnswer::getAnswerText)
                .map(this::normalize)
                .anyMatch(ans -> ans.equals(normalizedUser));
    }

    @Cacheable(cacheNames = "question", key = "#questionId", sync = true)
    private Question getQuestionCached(Long questionId) {
        return questionMapper.findById(questionId)
                .orElseThrow(() -> new BusinessException(ErrorCode.QUESTION_NOT_FOUND));
    }

    @Cacheable(cacheNames = "questionAnswers", key = "#questionId", sync = true)
    private List<QuestionAnswer> getQuestionAnswersCached(Long questionId) {
        return questionAnswerMapper.findByQuestionId(questionId);
    }

    private String normalize(String value) {
        return value == null ? "" : value.trim().toLowerCase(Locale.ROOT);
    }

    private void preloadQuestionCaches(List<Question> questions, Long quizId) {
        Cache questionCache = cacheManager.getCache("question");
        Cache answerCache = cacheManager.getCache("questionAnswers");
        if (questionCache == null || answerCache == null) {
            return;
        }

        // 질문 캐시 채우기
        for (Question q : questions) {
            questionCache.put(q.getId(), q);
        }

        // 퀴즈 단위로 정답을 한 번에 조회해 캐시에 채움
        List<QuestionAnswer> answers = questionAnswerMapper.findByQuizId(quizId);
        // 문제 ID별 리스트 구성
        java.util.Map<Long, java.util.List<QuestionAnswer>> grouped = new java.util.HashMap<>();
        for (QuestionAnswer a : answers) {
            grouped.computeIfAbsent(a.getQuestionId(), k -> new java.util.ArrayList<>()).add(a);
        }
        // 캐시에 저장 (빈 정답도 빈 리스트로 처리하여 캐시 미스 방지)
        for (Question q : questions) {
            java.util.List<QuestionAnswer> list = grouped.getOrDefault(q.getId(), java.util.List.of());
            answerCache.put(q.getId(), list);
        }
    }
    @Override
    @Transactional
    public Long createSubmission(Long quizId, Long userId, Long challengeId) {
        int totalQuestions = questionMapper.findByQuizId(quizId).size();

        Submission submission = new Submission();
        submission.setQuizId(quizId);
        submission.setUserId(userId);
        submission.setChallengeId(challengeId);
        submission.setTotalQuestions(totalQuestions);
        submission.setCorrectCount(0);
        submission.setSubmittedAt(TimeUtils.now()); // Start time
        submission.setPlayTime(0.0);

        submissionMapper.insertSubmission(submission);
        return submission.getId();
    }
}
