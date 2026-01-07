package com.problemio.ranking.controller;

import com.problemio.ranking.domain.RankingPeriod;
import com.problemio.ranking.dto.RankingResponseDto;
import com.problemio.ranking.service.RankingService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/rankings")
public class RankingController {

    private final RankingService rankingService;

    public RankingController(RankingService rankingService) {
        this.rankingService = rankingService;
    }

    @GetMapping
    public List<RankingResponseDto> getRanking(
            @RequestParam(name = "period", defaultValue = "TODAY") String period,
            @RequestParam(name = "limit", defaultValue = "20") int limit
    ) {
        RankingPeriod p = RankingPeriod.valueOf(period.toUpperCase());
        return rankingService.getRanking(p, limit);
    }
}
