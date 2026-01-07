package com.problemio.comment.mapper;

import com.problemio.comment.domain.Comment;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.time.LocalDateTime;
import java.util.List;

@Mapper
public interface CommentMapper {

    void insertComment(Comment comment);

    void updateComment(Comment comment);

    void softDeleteComment(@Param("id") Long id, @Param("updatedAt") LocalDateTime updatedAt);

    void updateRootCommentId(@Param("id") Long id, @Param("rootCommentId") Long rootCommentId);

    Comment findById(@Param("id") Long id);

    List<Comment> findRootCommentsByQuizId(
            @Param("quizId") Long quizId,
            @Param("limit") int limit,
            @Param("offset") int offset
    );

    List<Comment> findRepliesByParentId(@Param("parentId") Long parentId);

    int countCommentsByQuizId(@Param("quizId") Long quizId);

    List<CommentReplyCount> countRepliesByParentIds(@Param("parentIds") List<Long> parentIds);

    List<CommentCount> countCommentsByQuizIds(@Param("quizIds") List<Long> quizIds);

    void increaseLikeCount(@Param("commentId") Long commentId);

    void decreaseLikeCount(@Param("commentId") Long commentId);

    List<Long> findIdsByUserId(@Param("userId") Long userId);

    List<Long> findIdsByQuizId(@Param("quizId") Long quizId);

    void softDeleteByUserId(@Param("userId") Long userId, @Param("updatedAt") LocalDateTime updatedAt);

    void softDeleteByQuizId(@Param("quizId") Long quizId, @Param("updatedAt") LocalDateTime updatedAt);

    void anonymizeByUserId(@Param("userId") Long userId, @Param("updatedAt") LocalDateTime updatedAt);

    void deleteByUserId(@Param("userId") Long userId);

    class CommentReplyCount {
        private Long parentId;
        private Integer count;

        public Long getParentId() { return parentId; }
        public void setParentId(Long parentId) { this.parentId = parentId; }
        public Integer getCount() { return count; }
        public void setCount(Integer count) { this.count = count; }
    }

    class CommentCount {
        private Long quizId;
        private Integer count;
        public Long getQuizId() { return quizId; }
        public void setQuizId(Long quizId) { this.quizId = quizId; }
        public Integer getCount() { return count; }
        public void setCount(Integer count) { this.count = count; }
    }
}
