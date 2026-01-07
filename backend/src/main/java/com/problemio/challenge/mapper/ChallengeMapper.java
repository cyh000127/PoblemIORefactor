package com.problemio.challenge.mapper;

import com.problemio.challenge.domain.Challenge;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;
import java.util.Optional;

@Mapper
public interface ChallengeMapper {
    List<Challenge> findAll();
    Optional<Challenge> findById(Long id);
    void insert(Challenge challenge);
}
