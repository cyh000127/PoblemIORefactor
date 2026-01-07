package com.problemio.ranking.mapper;

import com.problemio.ranking.dto.RankingRowDto;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.time.LocalDateTime;
import java.util.List;

@Mapper
public interface RankingMapper {
    List<RankingRowDto> findRankingInPeriod(
            @Param("start") LocalDateTime start,
            @Param("end") LocalDateTime end,
            @Param("limit") int limit
    );
}
