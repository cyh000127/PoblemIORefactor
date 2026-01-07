package com.problemio.challenge.service;

import com.problemio.challenge.dto.ChallengeDto;
import com.problemio.challenge.dto.ChallengeStartResponse;
import com.problemio.challenge.dto.ChallengeResultResponse;
import com.problemio.submission.dto.QuizSubmissionRequest;
import com.problemio.submission.dto.QuizAnswerResponse;

import java.util.List;

public interface ChallengeService {

    List<ChallengeDto> getChallenges();

    ChallengeDto getChallenge(Long challengeId);

    ChallengeStartResponse startChallenge(Long userId, Long challengeId);

    QuizAnswerResponse submitAnswer(Long userId, Long challengeId, QuizSubmissionRequest request);

    ChallengeResultResponse getChallengeResult(Long userId, Long challengeId);

    void finalizeChallenge(Long challengeId);

    List<com.problemio.challenge.dto.ChallengeRankingResponse> getTopRankings(Long challengeId);

    com.problemio.challenge.dto.LeaderboardResponse getLeaderboard(Long challengeId, Long userId);
}
