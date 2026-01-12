package com.problemio.user.repository;

import com.problemio.user.domain.DeleteStatus;
import com.problemio.user.domain.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface UserRepository extends JpaRepository<User, Long> {
    Optional<User> findByIdAndIsDeleted(Long id, DeleteStatus isDeleted);

    // 이메일로 사용자 조회
    Optional<User> findByEmail(String email);
    Optional<User> findByNickname(String nickname);

    // 중복 검사
    boolean existsByEmail(String email);
    boolean existsByNickname(String nickname);
    boolean existsByNicknameAndIsDeleted(String nickname, DeleteStatus isDeleted);

    //== 통계용 쿼리 ==//
    // 일단은 User에서 전부 해결 -> 나중에 모듈 리팩토링 범위 내에서 해결

    // 팔로워 수 ( 나를 팔로우 하는 사람 )
    @Query(value = "SELECT COUNT(*) "+
            "FROM follows f " +
            "JOIN users u " +
            "ON u.id = f.follwer_id " +
            "WHERE f.following_id = :userId " +
            "AND u.is_deleted = 'ACTIVE'"
            ,nativeQuery = true)
    int countFollwers(@Param("userId") Long userId);

    // 팔로잉 수 ( 내가 팔로우 하는 사람 )
    @Query(value = "SELECT COUNT(*) " +
            "FROM follows f " +
            "JOIN users u " +
            "ON u.id = f.following_id " +
            "WHERE f.follower_id = :userId " +
            "AND u.is_deleted = 'ACTIVE'"
            , nativeQuery = true)
    int countFollowings(@Param("userId") Long userId);

    // 이메일로 조회 및 삭제 여부 확인
    Optional<User> findByEmailAndIsDeleted(String email, DeleteStatus isDeleted);

    // 닉네임으로 조회 및 삭제 여부 확인
    Optional<User> findByNicknameAndIsDeleted(String nickname, DeleteStatus isDeleted);

    // 팔로우 여부 확인 (Native Query)
    @Query(value = "SELECT COUNT(*) FROM follows WHERE follower_id = :followerId AND following_id = :followingId", nativeQuery = true)
    int countByFollowerIdAndFollowingId(@Param("followerId") Long followerId, @Param("followingId") Long followingId);

    // 생성한 퀴즈 수 확인 (Native Query)
    @Query(value = "SELECT COUNT(*) FROM quizzes WHERE user_id = :userId", nativeQuery = true)
    long countCreatedQuizzes(@Param("userId") Long userId);



}
