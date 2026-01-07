package com.problemio.comment.mapper;

import com.problemio.comment.domain.CommentLike;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

@Mapper
public interface CommentLikeMapper {

    void insert(CommentLike commentLike);

    void delete(@Param("userId") Long userId, @Param("commentId") Long commentId);

    boolean exists(
            @Param("userId") Long userId,
            @Param("commentId") Long commentId
    );

    /**
     * 로그인 유저가 좋아요한 댓글 id 목록을 한번에 조회
     */
    java.util.List<Long> findLikedCommentIds(
            @Param("userId") Long userId,
            @Param("commentIds") java.util.List<Long> commentIds
    );

    void deleteByUserId(@Param("userId") Long userId);

    java.util.List<Long> findLikedCommentIdsByUser(@Param("userId") Long userId);

    void deleteByCommentIds(@Param("commentIds") java.util.List<Long> commentIds);
}
