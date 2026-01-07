package com.problemio.user.mapper;

import com.problemio.user.domain.User;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.Optional;

@Mapper
public interface UserAuthMapper {

    // Id로 찾기
    Optional<User> findById(@Param("id") Long id);

    // 이메일 검색
    Optional<User> findByEmail(@Param("email") String email);

    // 닉네임 검색
    Optional<User> findByNickname(@Param("nickname") String nickname);

    // 회원가입
    void insertUser(User user);
}