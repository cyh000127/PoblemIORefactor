package com.problemio.submission.mapper;

import com.problemio.submission.domain.SubmissionDetail;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;
import java.util.Optional;

@Mapper
public interface SubmissionDetailMapper {

    void insertSubmissionDetail(SubmissionDetail detail);

    void insertSubmissionDetails(@Param("details") List<SubmissionDetail> details);

    List<SubmissionDetail> findBySubmissionId(@Param("submissionId") Long submissionId);

    int countBySubmissionId(@Param("submissionId") Long submissionId);
    int countCorrectBySubmissionId(@Param("submissionId") Long submissionId);
    int countBySubmissionIdForUpdate(@Param("submissionId") Long submissionId);

    Optional<SubmissionDetail> findBySubmissionIdAndQuestionId(@Param("submissionId") Long submissionId,
                                                               @Param("questionId") Long questionId);

    void deleteBySubmissionIdAndQuestionId(@Param("submissionId") Long submissionId,
                                           @Param("questionId") Long questionId);

    void deleteByQuestionIds(@Param("questionIds") List<Long> questionIds);

    void deleteBySubmissionIds(@Param("submissionIds") List<Long> submissionIds);
}
