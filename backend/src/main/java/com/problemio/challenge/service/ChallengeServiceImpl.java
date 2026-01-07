package com.problemio.challenge.service;

import com.problemio.challenge.domain.Challenge;
import com.problemio.challenge.domain.ChallengeRanking;
import com.problemio.challenge.dto.ChallengeDto;
import com.problemio.challenge.dto.ChallengeRankingResponse;
import com.problemio.challenge.dto.ChallengeResultResponse;
import com.problemio.challenge.dto.ChallengeStartResponse;
import com.problemio.challenge.dto.LeaderboardResponse;
import com.problemio.challenge.mapper.ChallengeMapper;
import com.problemio.challenge.mapper.ChallengeRankingMapper;
import com.problemio.global.exception.BusinessException;
import com.problemio.global.exception.ErrorCode;
import com.problemio.quiz.dto.QuestionResponse;
import com.problemio.quiz.mapper.QuestionMapper;
import com.problemio.submission.domain.Submission;
import com.problemio.submission.dto.QuizAnswerResponse;
import com.problemio.submission.dto.QuizSubmissionRequest;
import com.problemio.submission.mapper.SubmissionMapper;
import com.problemio.submission.service.SubmissionService;
import lombok.RequiredArgsConstructor;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

import com.problemio.global.util.TimeUtils;

@Service
@RequiredArgsConstructor
public class ChallengeServiceImpl implements ChallengeService {

    private final ChallengeMapper challengeMapper;
    private final SubmissionService submissionService;
    private final SubmissionMapper submissionMapper; // 제출 로직 전반 검증용
    private final QuestionMapper questionMapper; 
    private final ChallengeRankingMapper challengeRankingMapper; // 챌린지 랭킹 전용

    @Override
    @Transactional(readOnly = true)
    public List<ChallengeDto> getChallenges() {
        return challengeMapper.findAll().stream()
                .map(this::toDto)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public ChallengeDto getChallenge(Long challengeId) {
        Challenge challenge = challengeMapper.findById(challengeId)
                .orElseThrow(() -> new BusinessException(ErrorCode.QUIZ_NOT_FOUND)); 
        return toDto(challenge);
    }

    @Override
    @Transactional
    public ChallengeStartResponse startChallenge(Long userId, Long challengeId) {
        Challenge challenge = challengeMapper.findById(challengeId)
                .orElseThrow(() -> new BusinessException(ErrorCode.QUIZ_NOT_FOUND));

        if (challenge.getEndAt() != null && TimeUtils.now().isAfter(challenge.getEndAt())) {
            throw new BusinessException(ErrorCode.ACCESS_DENIED);
        }

        Long submissionId = submissionService.createSubmission(challenge.getTargetQuizId(), userId, challengeId);

        List<QuestionResponse> questions = questionMapper.findByQuizId(challenge.getTargetQuizId()).stream()
                .map(q -> QuestionResponse.builder()
                        .id(q.getId())
                        .order(q.getQuestionOrder())
                        .imageUrl(q.getImageUrl())
                        .description(q.getDescription())
                        .build())
                .collect(Collectors.toList());

        Collections.shuffle(questions);


        return ChallengeStartResponse.builder()
                .challengeId(challenge.getId())
                .submissionId(submissionId)
                .challengeType(challenge.getChallengeType())
                .timeLimit(challenge.getTimeLimit())
                .questions(questions)
                .build();
    }

    @Override
    @Transactional
    public QuizAnswerResponse submitAnswer(Long userId, Long challengeId, QuizSubmissionRequest request) {
        Challenge challenge = challengeMapper.findById(challengeId)
                .orElseThrow(() -> new BusinessException(ErrorCode.QUIZ_NOT_FOUND));

        // 타임어택 검증
        if ("TIME_ATTACK".equals(challenge.getChallengeType()) && request.getSubmissionId() != null) {
             Submission submission = submissionMapper.findById(request.getSubmissionId())
                     .orElseThrow(() -> new BusinessException(ErrorCode.ACCESS_DENIED));
             
             if (submission.getSubmittedAt() != null) {
                 long diffSeconds = java.time.Duration.between(submission.getSubmittedAt(), TimeUtils.now()).getSeconds();
                 // 네트워크 지연 고려 5초 버퍼
                 if (diffSeconds > challenge.getTimeLimit() + 5) { 
                     throw new BusinessException(ErrorCode.ACCESS_DENIED); 
                 }
             }
        }

        return submissionService.submitQuiz(challenge.getTargetQuizId(), userId, request);
    }

    @Override
    @Transactional(readOnly = true)
    @Cacheable(value = "ranking", key = "#challengeId + '-' + #userId")
    public ChallengeResultResponse getChallengeResult(Long userId, Long challengeId) {
        Submission submission = challengeRankingMapper.findSubmissionByUserIdAndChallengeId(userId, challengeId);
        
        if (submission == null) {
            throw new BusinessException(ErrorCode.ACCESS_DENIED);
        }

        int rank = challengeRankingMapper.getLiveRanking(
            challengeId, 
            submission.getCorrectCount(), 
            submission.getPlayTime(), 
            submission.getSubmittedAt()
        );

        double playTimeSeconds = submission.getPlayTime() != null ? submission.getPlayTime() : 0.0;
        String formattedTime = String.format("%.3f", playTimeSeconds);

        return ChallengeResultResponse.builder()
                .challengeId(challengeId)
                .submissionId(submission.getId())
                .rank(rank)
                .correctCount(submission.getCorrectCount())
                .playTime(submission.getPlayTime())
                .formattedTime(formattedTime)
                .challengeType(challengeMapper.findById(challengeId).map(Challenge::getChallengeType).orElse("UNKNOWN"))
                .build();
    }

    @Override
    @Transactional
    public void finalizeChallenge(Long challengeId) {
        // 1. 전체 제출 내역 조회
        List<Submission> submissions = challengeRankingMapper.findSubmissionsByChallengeId(challengeId);

        if (submissions.isEmpty()) {
            return;
        }

        // 2. 사용자별 최고 기록 필터링 (Map 사용)
        java.util.Map<Long, Submission> bestSubmissionsMap = new java.util.HashMap<>();
        
        for (Submission s : submissions) {
            if (!bestSubmissionsMap.containsKey(s.getUserId())) {
                bestSubmissionsMap.put(s.getUserId(), s);
            } else {
                Submission existing = bestSubmissionsMap.get(s.getUserId());
                // 비교: 정답수 내림차순, 소요시간 오름차순, 제출일시 오름차순
                boolean isBetter = false;
                if (s.getCorrectCount() > existing.getCorrectCount()) {
                    isBetter = true;
                } else if (s.getCorrectCount() == existing.getCorrectCount()) {
                    if (s.getPlayTime() < existing.getPlayTime()) {
                        isBetter = true;
                    } else if (s.getPlayTime().equals(existing.getPlayTime())) {
                        if (s.getSubmittedAt().isBefore(existing.getSubmittedAt())) {
                            isBetter = true;
                        }
                    }
                }
                
                if (isBetter) {
                    bestSubmissionsMap.put(s.getUserId(), s);
                }
            }
        }
        
        List<Submission> bestSubmissions = new java.util.ArrayList<>(bestSubmissionsMap.values());

        // 3. 최고 기록 정렬
        bestSubmissions.sort((s1, s2) -> {
            if (s1.getCorrectCount() != s2.getCorrectCount()) {
                return s2.getCorrectCount() - s1.getCorrectCount(); 
            }
            if (!s1.getPlayTime().equals(s2.getPlayTime())) {
                return Double.compare(s1.getPlayTime(), s2.getPlayTime());
            }
            return s1.getSubmittedAt().compareTo(s2.getSubmittedAt()); 
        });

        // 4. 랭킹 엔티티 생성
        List<ChallengeRanking> rankings = new java.util.ArrayList<>();
        for (int i = 0; i < bestSubmissions.size(); i++) {
            Submission s = bestSubmissions.get(i);
            
            ChallengeRanking ranking = new ChallengeRanking();
            ranking.setChallengeId(challengeId);
            ranking.setUserId(s.getUserId());
            ranking.setRanking(i + 1);
            ranking.setScore((double) s.getCorrectCount());
            ranking.setPlayTime(s.getPlayTime());
            ranking.setCreatedAt(TimeUtils.now());
            
            rankings.add(ranking);
        }

        // 5. 기존 랭킹 초기화 및 저장
        challengeRankingMapper.deleteRankingsByChallengeId(challengeId);
        challengeRankingMapper.insertRankings(rankings);
    }

    @Override
    @Transactional // 지연 확정(쓰기) 발생 가능하므로 readOnly 제외
    @Cacheable(value = "leaderboard", key = "#challengeId")
    public List<ChallengeRankingResponse> getTopRankings(Long challengeId) {
        // DTO에 챌린지 타입 포함
        List<ChallengeRankingResponse> topRankings = resolveTopRankings(challengeId);
        
        String type = challengeMapper.findById(challengeId).map(Challenge::getChallengeType).orElse("UNKNOWN");
        topRankings.forEach(r -> r.setChallengeType(type));
        
        return topRankings;
    }

    @Override
    @Transactional // readOnly 제외
    public LeaderboardResponse getLeaderboard(Long challengeId, Long userId) {
        // 1. 상위 랭킹 조회 (필요 시 지연 확정)
        List<ChallengeRankingResponse> topRankings = resolveTopRankings(challengeId);
        
        String type = challengeMapper.findById(challengeId).map(Challenge::getChallengeType).orElse("UNKNOWN");
        topRankings.forEach(r -> r.setChallengeType(type));

        // 2. 내 랭킹 조회
        ChallengeRankingResponse myRanking = null;
        if (userId != null) {
            myRanking = findMyBestRanking(userId, challengeId, type);
        }
        
        // 기록 없음 시 0점 반환 ("10등 + 내 기록 0점" 요청 반영)
        if (myRanking == null) {
             myRanking = ChallengeRankingResponse.builder()
                .challengeId(challengeId)
                .userId(userId) // 게스트인 경우 null
                .ranking(0)
                .score(0.0)
                .playTime(0.0)
                .nickname(userId == null ? "Guest" : "Me")
                .challengeType(type)
                .build();
        }

        return LeaderboardResponse.builder()
                .topRankings(topRankings)
                .myRanking(myRanking)
                .build();
    }
    
    // 진행중/종료 상태 무관하게 내 최고 랭킹 조회
    private ChallengeRankingResponse findMyBestRanking(Long userId, Long challengeId, String challengeType) {
         boolean isExpired = isChallengeExpired(challengeId);
         
         if (isExpired) {
             // 1. 아카이브(종료됨) 확인
             ChallengeRankingResponse ranking = challengeRankingMapper.loginUserRanking(userId, challengeId);
             if (ranking != null) {
                 ranking.setChallengeType(challengeType);
                 return ranking;
             }
             // 아카이브에 없으면 안전장치로 라이브 데이터 확인
         }
         
         // 2. 라이브 데이터 확인 (진행중 또는 폴백)
         Submission s = challengeRankingMapper.findSubmissionByUserIdAndChallengeId(userId, challengeId);
         if (s != null) {
             int rank = challengeRankingMapper.getLiveRanking(
                 challengeId, 
                 s.getCorrectCount(), 
                 s.getPlayTime(), 
                 s.getSubmittedAt()
             );
             
             return ChallengeRankingResponse.builder()
                     .challengeId(challengeId)
                     .userId(userId)
                     .ranking(rank)
                     .nickname("Me")
                     .score((double) s.getCorrectCount())
                     .playTime(s.getPlayTime())
                     .challengeType(challengeType)
                     .build();
         }
         
         return null;
    }
    
    // 지연 확정을 위한 헬퍼 메서드
    private List<ChallengeRankingResponse> resolveTopRankings(Long challengeId) {
        Challenge challenge = challengeMapper.findById(challengeId)
                .orElseThrow(() -> new BusinessException(ErrorCode.QUIZ_NOT_FOUND));
        
        boolean isExpired = challenge.getEndAt() != null && TimeUtils.now().isAfter(challenge.getEndAt());
        
        if (isExpired) {
            // 아카이브 여부 확인
            if (!challengeRankingMapper.existsByChallengeId(challengeId)) {
                // 지연 확정 실행
                finalizeChallenge(challengeId);
            }
            // 아카이브 데이터 반환
            return challengeRankingMapper.challengeTotalRanking(challengeId, 10);
        } else {
            // 라이브 데이터 반환
            return challengeRankingMapper.findLiveTopRankingsByChallengeId(challengeId, 10);
        }
    }
    
    private boolean isChallengeExpired(Long challengeId) {
        Challenge challenge = challengeMapper.findById(challengeId).orElse(null);
        return challenge != null && challenge.getEndAt() != null && TimeUtils.now().isAfter(challenge.getEndAt());
    }

    private final com.problemio.quiz.mapper.QuizMapper quizMapper;

    private ChallengeDto toDto(Challenge challenge) {
        // 타겟 퀴즈 조회
        com.problemio.quiz.domain.Quiz quiz = quizMapper.findById(challenge.getTargetQuizId()).orElse(null);
        com.problemio.quiz.dto.QuizResponse quizResponse = null;
        
        if (quiz != null) {
            quizResponse = com.problemio.quiz.dto.QuizResponse.builder()
                .id(quiz.getId())
                .title(quiz.getTitle())
                .thumbnailUrl(quiz.getThumbnailUrl())
                // 썸네일 표시에 필요한 기본 필드만 포함
                .build();
        }

        return ChallengeDto.builder()
                .id(challenge.getId())
                .title(challenge.getTitle())
                .description(challenge.getDescription())
                .challengeType(challenge.getChallengeType())
                .timeLimit(challenge.getTimeLimit())
                .startAt(challenge.getStartAt())
                .endAt(challenge.getEndAt())
                .targetQuiz(quizResponse)
                .build();
    }
}
