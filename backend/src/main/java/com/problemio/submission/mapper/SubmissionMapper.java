package com.problemio.submission.mapper;

import com.problemio.submission.domain.Submission;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;
import java.util.Optional;

import java.time.LocalDateTime;

@Mapper
public interface SubmissionMapper {

    void insertSubmission(Submission submission);

    void updateCorrectCount(@Param("id") Long id,
                            @Param("quizId") Long quizId,
                            @Param("correctCount") int correctCount);

    void updatePlayTime(@Param("id") Long id,
                        @Param("quizId") Long quizId,
                        @Param("playTime") Double playTime);

    void updatePlayTimeNow(@Param("id") Long id, @Param("quizId") Long quizId, @Param("now") LocalDateTime now);




    Optional<Submission> findById(@Param("id") Long id);

    List<Submission> findByQuizId(@Param("quizId") Long quizId);

    List<Submission> findByUserId(@Param("userId") Long userId);

    void deleteByQuizId(@Param("quizId") Long quizId);

    List<Long> findIdsByUserId(@Param("userId") Long userId);

    void deleteByUserId(@Param("userId") Long userId);
}
