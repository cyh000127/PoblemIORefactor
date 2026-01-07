package com.problemio.quiz.mapper;

import com.problemio.quiz.domain.QuestionAnswer;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface QuestionAnswerMapper {

    void insertAnswer(QuestionAnswer answer);

    void deleteByQuestionId(@Param("questionId") Long questionId);

    List<QuestionAnswer> findByQuestionId(@Param("questionId") Long questionId);

    List<QuestionAnswer> findByQuestionIds(@Param("questionIds") List<Long> questionIds);

    List<QuestionAnswer> findByQuizId(@Param("quizId") Long quizId);
}
