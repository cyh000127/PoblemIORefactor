package com.problemio.quiz.service;

import com.problemio.follow.mapper.FollowMapper;
import com.problemio.global.exception.BusinessException;
import com.problemio.global.exception.ErrorCode;
import com.problemio.quiz.domain.Question;
import com.problemio.quiz.domain.QuestionAnswer;
import com.problemio.quiz.dto.AnswerCreateRequest;
import com.problemio.quiz.dto.QuestionAnswerDto;
import com.problemio.quiz.dto.QuestionCreateRequest;
import com.problemio.quiz.dto.QuestionResponse;
import com.problemio.quiz.mapper.QuestionAnswerMapper;
import com.problemio.quiz.mapper.QuestionMapper;
import com.problemio.quiz.domain.Quiz;
import com.problemio.quiz.dto.QuizCreateRequest;
import com.problemio.quiz.dto.QuizResponse;
import com.problemio.quiz.dto.QuizSummaryDto;
import com.problemio.quiz.dto.QuizUpdateRequest;
import com.problemio.quiz.mapper.QuizLikeMapper;
import com.problemio.quiz.mapper.QuizMapper;
import com.problemio.submission.mapper.SubmissionDetailMapper;
import com.problemio.submission.mapper.SubmissionMapper;
import com.problemio.comment.mapper.CommentLikeMapper;
import com.problemio.user.dto.UserResponse;
import com.problemio.user.mapper.UserMapper;
import com.problemio.comment.mapper.CommentMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

import com.problemio.global.util.TimeUtils;

@Service
@RequiredArgsConstructor
@Transactional
public class QuizServiceImpl implements QuizService {

    // 퀴즈 기본 정보, 좋아요, 문제, 정답 매퍼
    private final QuizMapper quizMapper;
    private final QuizLikeMapper quizLikeMapper;
    private final QuestionMapper questionMapper;
    private final QuestionAnswerMapper questionAnswerMapper;
    // 유저, 팔로우, 제출 관련 매퍼
    private final UserMapper userMapper;
    private final FollowMapper followMapper;
    private final SubmissionMapper submissionMapper;
    private final SubmissionDetailMapper submissionDetailMapper;
    // 댓글 관련 매퍼
    private final CommentMapper commentMapper;
    private final CommentLikeMapper commentLikeMapper;
    // 캐시 관리
    private final CacheManager cacheManager;

    /**
     * 퀴즈 목록 조회 (페이징, 정렬, 검색)
     */
    @Override
    @Transactional(readOnly = true)
    public Map<String, Object> getQuizzes(int page, int size, String sort, String keyword) {
        int safePage = Math.max(page, 1);   // 최소 1페이지 보장
        int safeSize = Math.max(size, 1);   // 최소 1건 이상 보장
        int offset = (safePage - 1) * safeSize;

        // 퀴즈 목록 검색 및 전체 개수 조회
        List<Quiz> quizzes = quizMapper.searchQuizzes(offset, safeSize, sort, keyword);
        int total = quizMapper.countQuizzes(keyword);

        Map<Long, Integer> commentCountMap = quizzes.isEmpty()
                ? Map.of()
                : commentMapper.countCommentsByQuizIds(quizzes.stream().map(Quiz::getId).toList())
                .stream()
                .collect(Collectors.toMap(CommentMapper.CommentCount::getQuizId, CommentMapper.CommentCount::getCount));

        // Quiz 엔티티를 요약 DTO로 변환
        List<QuizSummaryDto> content = quizzes.stream()
                .map(q -> toSummaryDto(q, commentCountMap))
                .collect(Collectors.toList());

        int totalPages = (int) Math.ceil((double) total / safeSize);

        // 목록 + 페이징 정보 응답
        return Map.of(
                "content", content,
                "totalPages", totalPages,
                "totalElements", total
        );
    }

    // ===== 퀴즈 생성/수정/삭제 =====
    @Override
    public QuizResponse createQuiz(Long userId, QuizCreateRequest request) {
        Quiz quiz = new Quiz();
        quiz.setUserId(userId);
        quiz.setTitle(request.getTitle());
        quiz.setDescription(request.getDescription());
        quiz.setThumbnailUrl(request.getThumbnailUrl());
        quiz.setPublic(request.isPublic());
        quiz.setLikeCount(0);
        quiz.setPlayCount(0);
        
        LocalDateTime now = TimeUtils.now();
        quiz.setCreatedAt(now);
        quiz.setUpdatedAt(now);

        // 퀴즈 삽입 -> 문제/정답 저장 -> 응답 생성
        quizMapper.insertQuiz(quiz);
        saveQuestions(quiz.getId(), request.getQuestions());

        return buildQuizResponse(quiz, null, null, null);
    }

    @Override
    public QuizResponse updateQuiz(Long userId, Long quizId, QuizUpdateRequest request) {
        Quiz quiz = quizMapper.findById(quizId)
                .orElseThrow(() -> new BusinessException(ErrorCode.QUIZ_NOT_FOUND));

        // 작성자 검증
        if (!Objects.equals(quiz.getUserId(), userId)) {
            throw new BusinessException(ErrorCode.ACCESS_DENIED);
        }

        // 필드 부분 업데이트
        if (request.getTitle() != null) {
            quiz.setTitle(request.getTitle());
        }
        if (request.getDescription() != null) {
            quiz.setDescription(request.getDescription());
        }
        if (request.getThumbnailUrl() != null) {
            quiz.setThumbnailUrl(request.getThumbnailUrl());
        }
        if (request.getIsPublic() != null) {
            quiz.setPublic(request.getIsPublic());
        }

        // 기본 정보 업데이트
        quiz.setUpdatedAt(TimeUtils.now());
        quizMapper.updateQuiz(quiz);

        // 질문 목록 재설정 (삭제 후 재생성)
        if (request.getQuestions() != null) {
            List<Question> existing = questionMapper.findByQuizId(quizId);
            // 기존 질문, 정답 삭제
            for (Question question : existing) {
                questionAnswerMapper.deleteByQuestionId(question.getId());
            }

            questionMapper.deleteByQuizId(quizId);
            // 새 질문/정답 저장
            saveQuestions(quizId, request.getQuestions());
        }

        // 응답 생성
        return buildQuizResponse(quiz, loadQuestions(quizId), findAuthor(quiz.getUserId()), null);
    }

    @Override
    public void deleteQuiz(Long userId, Long quizId) {
        Quiz quiz = quizMapper.findById(quizId)
                .orElseThrow(() -> new BusinessException(ErrorCode.QUIZ_NOT_FOUND));

        // 작성자 검증
        if (!Objects.equals(quiz.getUserId(), userId)) {
            throw new BusinessException(ErrorCode.ACCESS_DENIED);
        }

        // 댓글 및 관련 데이터 삭제
        List<Long> commentIds = commentMapper.findIdsByQuizId(quizId);
        if (!commentIds.isEmpty()) {
            commentLikeMapper.deleteByCommentIds(commentIds);
            commentMapper.softDeleteByQuizId(quizId, TimeUtils.now());
        }

        // 퀴즈 좋아요 삭제
        quizLikeMapper.deleteByQuizId(quizId);

        // 제출 기록 삭제 (FK 제약 고려)
        List<Question> questions = questionMapper.findByQuizId(quizId);
        List<Long> questionIds = questions.stream()
                .map(Question::getId)
                .collect(Collectors.toList());
        if (!questionIds.isEmpty()) {
            submissionDetailMapper.deleteByQuestionIds(questionIds);
        }
        submissionMapper.deleteByQuizId(quizId);

        // 질문 및 정답 삭제
        for (Question question : questions) {
            questionAnswerMapper.deleteByQuestionId(question.getId());
        }
        questionMapper.deleteByQuizId(quizId);

        // 퀴즈 삭제
        quizMapper.deleteQuiz(quizId);
    }

    // ===== 퀴즈 조회 =====
    @Override
    @Transactional
    public QuizResponse getQuiz(Long quizId, Long viewerId) {
        Quiz quiz = quizMapper.findById(quizId)
                .orElseThrow(() -> new BusinessException(ErrorCode.QUIZ_NOT_FOUND));

        // 조회수 증가
        quizMapper.incrementPlayCount(quizId);
        quiz.setPlayCount(quiz.getPlayCount() + 1);

        UserResponse author = findAuthor(quiz.getUserId());
        List<QuestionResponse> questions = loadQuestions(quizId);

        Boolean isLikedByMe = null;
        Boolean isFollowedByMe = null;

        // 상태 정보 확인 (좋아요, 팔로우)
        if (viewerId != null) {
            isLikedByMe = quizLikeMapper.findByUserIdAndQuizId(viewerId, quizId).isPresent();
            isFollowedByMe = followMapper.exists(viewerId, quiz.getUserId()) > 0;
        }

        return buildQuizResponse(quiz, questions, author, isLikedByMe, isFollowedByMe);
    }

    // ===== 공개 목록 조회 =====
    @Override
    @Transactional(readOnly = true)
    public List<QuizSummaryDto> getPublicQuizzes() {
        return quizMapper.findPublicQuizzes().stream()
                .map(this::toSummaryDto)
                .collect(Collectors.toList());
    }

    /**
     * 특정 유저의 퀴즈 목록 조회
     */
    @Override
    @Transactional(readOnly = true)
    public List<QuizSummaryDto> getUserQuizzes(Long userId) {
        return quizMapper.findQuizzesByUserId(userId).stream()
                .map(this::toSummaryDto)
                .collect(Collectors.toList());
    }

    /**
     * 퀴즈 좋아요
     */
    @Override
    public void likeQuiz(Long userId, Long quizId) {
        // 퀴즈 조회
        Quiz quiz = quizMapper.findById(quizId)
                .orElseThrow(() -> new BusinessException(ErrorCode.QUIZ_NOT_FOUND));

        // 본인 퀴즈 좋아요 불가
        if (Objects.equals(quiz.getUserId(), userId)) {
            throw new BusinessException(ErrorCode.CANNOT_LIKE_OWN_QUIZ);
        }

        // 이미 좋아요 여부 확인
        boolean alreadyLiked = quizLikeMapper.findByUserIdAndQuizId(userId, quizId).isPresent();
        if (alreadyLiked) {
            return; 
        }

        // 좋아요 레코드 생성 후 카운트 증가
        quizLikeMapper.insertQuizLike(buildQuizLike(userId, quizId));
        quizMapper.incrementLikeCount(quizId);
    }

    /**
     * 퀴즈 좋아요 취소
     */
    @Override
    public void unlikeQuiz(Long userId, Long quizId) {
        quizMapper.findById(quizId)
                .orElseThrow(() -> new BusinessException(ErrorCode.QUIZ_NOT_FOUND));

        quizLikeMapper.deleteQuizLike(userId, quizId);
        quizMapper.decrementLikeCount(quizId);
    }

    // ===== DTO 변환 헬퍼 =====
    private QuizSummaryDto toSummaryDto(Quiz quiz, Map<Long, Integer> commentCountMap) {
        int commentCount = commentCountMap != null
                ? commentCountMap.getOrDefault(quiz.getId(), 0)
                : 0;
        return QuizSummaryDto.builder()
                .id(quiz.getId())
                .title(quiz.getTitle())
                .description(quiz.getDescription())
                .thumbnailUrl(quiz.getThumbnailUrl())
                .likeCount(quiz.getLikeCount())
                .playCount(quiz.getPlayCount())
                .commentCount(commentCount)
                .hidden(quiz.isHidden())
                .build();
    }

    // 댓글 수 정보가 없을 때의 호환용 메서드
    private QuizSummaryDto toSummaryDto(Quiz quiz) {
        return toSummaryDto(quiz, Map.of());
    }

    /**
     * 응답 객체 생성 (팔로우 여부 미포함)
     */
    private QuizResponse buildQuizResponse(Quiz quiz,
                                           List<QuestionResponse> questions,
                                           UserResponse author,
                                           Boolean isLikedByMe) {
        return buildQuizResponse(quiz, questions, author, isLikedByMe, null);
    }

    /**
     * 응답 객체 생성 (공통)
     */
    private QuizResponse buildQuizResponse(Quiz quiz,
                                           List<QuestionResponse> questions,
                                           UserResponse author,
                                           Boolean isLikedByMe,
                                           Boolean isFollowedByMe) {
        return QuizResponse.builder()
                .id(quiz.getId())
                .userId(quiz.getUserId())
                .title(quiz.getTitle())
                .description(quiz.getDescription())
                .thumbnailUrl(quiz.getThumbnailUrl())
                .isPublic(quiz.isPublic())
                .likeCount(quiz.getLikeCount())
                .playCount(quiz.getPlayCount())
                .questions(questions)
                .author(author)
                .isLikedByMe(isLikedByMe)
                .isFollowedByMe(isFollowedByMe)
                .build();
    }

    // ===== 질문/정답 저장 및 로딩 헬퍼 =====
    private void saveQuestions(Long quizId, List<QuestionCreateRequest> questions) {
        if (questions == null || questions.isEmpty()) {
            return;
        }

        int index = 1;
        for (QuestionCreateRequest request : questions) {
            Question question = new Question();
            question.setQuizId(quizId);
            // 순서 미지정 시 인덱스 사용
            int order = request.getQuestionOrder() != null ? request.getQuestionOrder() : index;
            question.setQuestionOrder(order);
            question.setImageUrl(request.getImageUrl());
            question.setDescription(request.getDescription());
            question.setCreatedAt(TimeUtils.now());

            questionMapper.insertQuestion(question);
            saveAnswers(question.getId(), request.getAnswers());
            index++;
        }
    }

    private void saveAnswers(Long questionId, List<AnswerCreateRequest> answers) {
        if (answers == null || answers.isEmpty()) {
            return;
        }

        int idx = 1;
        for (AnswerCreateRequest answerRequest : answers) {
            // 빈 정답 제외
            if (answerRequest.getAnswerText() == null || answerRequest.getAnswerText().isBlank()) {
                continue;
            }

            QuestionAnswer answer = new QuestionAnswer();
            answer.setQuestionId(questionId);
            answer.setAnswerText(answerRequest.getAnswerText());
            answer.setAnswerText(answerRequest.getAnswerText());
            answer.setSortOrder(answerRequest.getSortOrder() != null ? answerRequest.getSortOrder() : idx);

            questionAnswerMapper.insertAnswer(answer);
            idx++;
        }
    }

    private List<QuestionResponse> loadQuestions(Long quizId) {
        List<Question> questions = questionMapper.findByQuizId(quizId);
        Map<Long, List<QuestionAnswerDto>> answersByQuestion = loadAnswersByQuestionIds(
                questions.stream().map(Question::getId).toList()
        );
        return questions.stream()
                .map(q -> QuestionResponse.builder()
                        .id(q.getId())
                        .order(q.getQuestionOrder())
                        .description(q.getDescription())
                        .imageUrl(q.getImageUrl())
                        .answers(answersByQuestion.getOrDefault(q.getId(), List.of()))
                        .build())
                .collect(Collectors.toList());
    }

    private List<QuestionResponse> mapQuestionsWithAnswers(List<Question> questions) {
        Map<Long, List<QuestionAnswerDto>> answersByQuestion = loadAnswersByQuestionIds(
                questions.stream().map(Question::getId).toList()
        );

        final int[] orderSeq = {1};
        return questions.stream()
                .map(q -> QuestionResponse.builder()
                        .id(q.getId())
                        .order(orderSeq[0]++)
                        .description(q.getDescription())
                        .imageUrl(q.getImageUrl())
                        .answers(answersByQuestion.getOrDefault(q.getId(), List.of()))
                        .build())
                .collect(Collectors.toList());
    }

    /**
     * 복수의 질문 ID에 대한 정답 조회
     */
    private Map<Long, List<QuestionAnswerDto>> loadAnswersByQuestionIds(List<Long> questionIds) {
        if (questionIds == null || questionIds.isEmpty()) {
            return Map.of();
        }

        List<QuestionAnswer> answers = questionAnswerMapper.findByQuestionIds(questionIds);

        return answers.stream()
                .collect(Collectors.groupingBy(
                        QuestionAnswer::getQuestionId,
                        Collectors.mapping(a -> {
                            QuestionAnswerDto dto = new QuestionAnswerDto();
                            dto.setId(a.getId());
                            dto.setAnswerText(a.getAnswerText());
                            dto.setSortOrder(a.getSortOrder());
                            return dto;
                        }, Collectors.toList())
                ));
    }

    private UserResponse findAuthor(Long userId) {
        Cache cache = cacheManager.getCache("userProfile");
        if (cache != null) {
            UserResponse cached = cache.get(userId, UserResponse.class);
            if (cached != null) {
                return cached;
            }
        }

        UserResponse author = userMapper.findById(userId).orElse(null);
        if (cache != null && author != null) {
            cache.put(userId, author);
        }
        return author;
    }

    /**
     * 좋아요 엔티티 생성 헬퍼
     */
    private com.problemio.quiz.domain.QuizLike buildQuizLike(Long userId, Long quizId) {
        com.problemio.quiz.domain.QuizLike like = new com.problemio.quiz.domain.QuizLike();
        like.setUserId(userId);
        like.setQuizId(quizId);
        like.setCreatedAt(TimeUtils.now());
        return like;
    }

    /*
     * 팔로우한 유저의 퀴즈 목록 조회
     */
    @Override
    @Transactional(readOnly = true)
    public List<QuizSummaryDto> getQuizzesOfFollowings(Long userId, int page, int size) {
        int offset = (page - 1) * size;
        return quizMapper.findQuizzesOfFollowings(userId, offset, size);
    }

    @Override
    @Transactional(readOnly = true)
    public List<QuizSummaryDto> getLikedQuizzes(Long userId, int page, int size) {
        int offset = (page - 1) * size;
        return quizMapper.findLikedQuizzesByUser(userId, offset, size);
    }

    @Override
    @Transactional(readOnly = true)
    public List<QuestionResponse> getQuizQuestions(Long quizId, Long viewerId, Integer limit) {
        Quiz quiz = quizMapper.findById(quizId)
                .orElseThrow(() -> new BusinessException(ErrorCode.QUIZ_NOT_FOUND));

        // 비공개 퀴즈 접근 제어
        if (!quiz.isPublic() && (viewerId == null || !quiz.getUserId().equals(viewerId))) {
            throw new BusinessException(ErrorCode.ACCESS_DENIED);
        }

        int safeLimit = normalizeLimit(limit);
        List<Question> questions = questionMapper.findRandomByQuizId(quizId, safeLimit);
        return mapQuestionsWithAnswers(questions);
    }

    private int normalizeLimit(Integer limit) {
        if (limit == null) return 50;
        int[] allowed = {10, 20, 30, 50};
        for (int val : allowed) {
            if (val == limit) return val;
        }
        // 근접한 상한 값으로 보정
        int closest = allowed[allowed.length - 1];
        for (int val : allowed) {
            if (limit <= val) {
                closest = val;
                break;
            }
        }
        return closest;
    }
}
