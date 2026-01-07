package com.problemio.ranking.service;

import com.problemio.ranking.domain.RankingPeriod;
import com.problemio.ranking.dto.RankingResponseDto;
import com.problemio.ranking.dto.RankingRowDto;
import com.problemio.ranking.mapper.RankingMapper;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Service
public class RankingServiceImpl implements RankingService {

    private final RankingMapper rankingMapper;

    private static final double ACCURACY_BASE = 0.3;   // weight floor
    private static final double ACCURACY_WEIGHT = 0.7; // weight for accuracy
    private static final double PRIOR_QUESTIONS = 20.0;
    private static final double PRIOR_ACCURACY = 0.6;
    private static final double FULL_QUESTION_COUNT = 50.0; // 선택지 최대값 (10/20/30/50)

    public RankingServiceImpl(RankingMapper rankingMapper) {
        this.rankingMapper = rankingMapper;
    }

    @Override
    @Cacheable(
            value = "ranking",
            key = "#period.name() + '_' + #limit",
            unless = "#result == null || #result.isEmpty()"
    )
    @Transactional(readOnly = true)
    public List<RankingResponseDto> getRanking(RankingPeriod period, int limit) {
        int topN = (limit > 0 && limit <= 100) ? limit : 20;

        LocalDate today = LocalDate.now();
        LocalDateTime start;
        LocalDateTime end;

        switch (period) {
            case TODAY -> {
                start = today.atStartOfDay();
                end = today.plusDays(1).atStartOfDay();
            }
            case YESTERDAY -> {
                start = today.minusDays(1).atStartOfDay();
                end = today.atStartOfDay();
            }
            case WEEK -> {
                LocalDate weekStart = today.with(DayOfWeek.MONDAY);
                start = weekStart.atStartOfDay();
                end = weekStart.plusWeeks(1).atStartOfDay();
            }
            default -> throw new IllegalArgumentException("Unsupported period: " + period);
        }

        List<RankingRowDto> rows = rankingMapper.findRankingInPeriod(start, end, topN);

        return rows.stream()
                .map(r -> RankingResponseDto.of(r, calcScore(r)))
                .sorted((a, b) -> Integer.compare(b.getScore(), a.getScore()))
                .toList();
    }

    private int calcScore(RankingRowDto r) {
        double smoothedAccuracy = calcSmoothedAccuracy(r.getTotalCorrect(), r.getTotalQuestions());
        double avgQuestionsPerQuiz = (r.getSolvedQuizCount() > 0)
                ? ((double) r.getTotalQuestions() / r.getSolvedQuizCount())
                : 0.0;
        // 문제 수 보정: 50개 풀 때 1.0, 10개 풀 때 0.2
        double questionAdjust = Math.min(1.0, avgQuestionsPerQuiz / FULL_QUESTION_COUNT);

        double score = r.getSolvedQuizCount()
                * (ACCURACY_BASE + ACCURACY_WEIGHT * smoothedAccuracy)
                * questionAdjust
                * 100;
        return (int) Math.round(score);
    }

    private double calcSmoothedAccuracy(int totalCorrect, int totalQuestions) {
        double correct = Math.max(0, totalCorrect);
        double questions = Math.max(0, totalQuestions);
        return (correct + PRIOR_QUESTIONS * PRIOR_ACCURACY) / (questions + PRIOR_QUESTIONS);
    }
}
