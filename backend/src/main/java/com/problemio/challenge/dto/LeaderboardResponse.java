package com.problemio.challenge.dto;

import lombok.Builder;
import lombok.Getter;

import java.util.List;

@Getter
@Builder
public class LeaderboardResponse {
    private List<ChallengeRankingResponse> topRankings;
    private ChallengeRankingResponse myRanking;
}
