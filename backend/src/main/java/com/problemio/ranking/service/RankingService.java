package com.problemio.ranking.service;

import com.problemio.ranking.domain.RankingPeriod;
import com.problemio.ranking.dto.RankingResponseDto;

import java.util.List;

public interface RankingService {
    List<RankingResponseDto> getRanking(RankingPeriod period, int limit);
}
