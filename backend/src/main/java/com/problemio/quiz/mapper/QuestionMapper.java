package com.problemio.quiz.mapper;

import com.problemio.quiz.domain.Question;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;
import java.util.Optional;

@Mapper
public interface QuestionMapper {

    void insertQuestion(Question question);

    void updateQuestion(Question question);

    void deleteQuestion(@Param("id") Long id);

    Optional<Question> findById(@Param("id") Long id);

    List<Question> findByQuizId(@Param("quizId") Long quizId);

    List<Question> findRandomByQuizId(@Param("quizId") Long quizId, @Param("limit") int limit);

    void deleteByQuizId(@Param("quizId") Long quizId);
}
