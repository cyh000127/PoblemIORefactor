package com.problemio.challenge.mapper;

import com.problemio.challenge.domain.ChallengeRanking;
import com.problemio.challenge.dto.ChallengeRankingResponse;
import com.problemio.submission.domain.Submission;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface ChallengeRankingMapper {
    // Basic CRUD
    void insertRanking(ChallengeRanking ranking);
    void insertRankings(@Param("rankings") List<ChallengeRanking> rankings);
    // findRankingsByChallengeId removed (Unused, use archived/live specifically)
    void deleteRankingsByChallengeId(@Param("challengeId") Long challengeId);
    
    // Archive Logic
    boolean existsByChallengeId(@Param("challengeId") Long challengeId);
    List<ChallengeRankingResponse> challengeTotalRanking(@Param("challengeId") Long challengeId, @Param("limit") int limit);
    ChallengeRankingResponse loginUserRanking(@Param("userId") Long userId, @Param("challengeId") Long challengeId);

    // Live Ranking (Calculated from Submissions table)
    int getLiveRanking(
        @Param("challengeId") Long challengeId, 
        @Param("correctCount") int correctCount, 
        @Param("playTime") Double playTime, 
        @Param("submittedAt") java.time.LocalDateTime submittedAt
    );

    List<ChallengeRankingResponse> findLiveTopRankingsByChallengeId(
        @Param("challengeId") Long challengeId, 
        @Param("limit") int limit
    );

    // Submission Helper Queries
    List<Submission> findSubmissionsByChallengeId(@Param("challengeId") Long challengeId);
    
    Submission findSubmissionByUserIdAndChallengeId(
        @Param("userId") Long userId, 
        @Param("challengeId") Long challengeId
    );
}
