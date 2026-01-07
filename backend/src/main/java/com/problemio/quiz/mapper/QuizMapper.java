package com.problemio.quiz.mapper;

import com.problemio.quiz.domain.Quiz;
import com.problemio.quiz.dto.QuizSummaryDto;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;
import java.util.Optional;

@Mapper
public interface QuizMapper {

    void insertQuiz(Quiz quiz);

    void updateQuiz(Quiz quiz);

    void deleteQuiz(@Param("id") Long id);

    Optional<Quiz> findById(@Param("id") Long id);

    List<Quiz> findPublicQuizzes();

    List<Quiz> findQuizzesByUserId(@Param("userId") Long userId);

    void incrementPlayCount(@Param("id") Long id);

    List<Quiz> searchQuizzes(@Param("offset") int offset,
                             @Param("size") int size,
                             @Param("sort") String sort,
                             @Param("keyword") String keyword);

    int countQuizzes(@Param("keyword") String keyword);

    void incrementLikeCount(@Param("id") Long id);

    void decrementLikeCount(@Param("id") Long id);

    // 퀴즈 작성자만 조회: 자기 퀴즈에 좋아요 못하게 막기 위함
    Long findUserIdByQuizId(@Param("id") Long id);

    // ================ 마이페이지에서 조회하기 위함 ================
    // 내가 팔로우한 유저들의 퀴즈 목록
    List<QuizSummaryDto> findQuizzesOfFollowings(
            @Param("userId") Long userId,
            @Param("offset") int offset,
            @Param("limit") int limit
    );

    // 내가 좋아요한 퀴즈 목록
    List<QuizSummaryDto> findLikedQuizzesByUser(
            @Param("userId") Long userId,
            @Param("offset") int offset,
            @Param("limit") int limit
    );

    // 관리자용 퀴즈 조회
    List<Quiz> findAdminQuizzes(
            @Param("offset") int offset,
            @Param("size") int size,
            @Param("keyword") String keyword
    );

    int countAdminQuizzes(@Param("keyword") String keyword);
}
