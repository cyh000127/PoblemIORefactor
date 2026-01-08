package com.problemio.user.domain;

import jakarta.persistence.*;
import lombok.*;
import org.springframework.boot.context.properties.bind.DefaultValue;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;

import com.problemio.user.domain.Role;


import java.time.LocalDateTime;

@Getter
@Entity
@Table(name="users")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class User {

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 50)
    private String email;

    @Column(nullable = false)
    private String passwordHash;

    @Column(nullable = false)
    private String nickname;

    @Column
    private String profileImageUrl;

    @Column
    private String statusMessage;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private DeleteStatus isDeleted = DeleteStatus.ACTIVE; //기본값 설정

    @CreatedDate
    @Column(updatable = false)
    private LocalDateTime createdAt ;

    @LastModifiedDate
    private LocalDateTime updatedAt;

    @Column
    @Enumerated(EnumType.STRING)
    private Role role = Role.ROLE_USER; // ROLE_USER, ROLE_ADMIN


    //== BUILDER ==//
    @Builder
    public User(String email, String password, String nickname, String profileImageUrl, String StatusMessage){
        this.email = email;
        this.passwordHash = password;
        this.nickname = nickname;
        this.profileImageUrl = profileImageUrl;
        this.statusMessage = StatusMessage;
    }


    // == 비즈니스 로직 ==//
    // 프로필 업데이트
    public void updateProfile(String nickname, String StatusMessage, String ProfileImageIrl){
        if(nickname != null) this.nickname = nickname;
        if(StatusMessage != null) this.statusMessage = StatusMessage;
        if(ProfileImageIrl != null) this.profileImageUrl = ProfileImageIrl;
    }

    // 비밀번호 변경
    public void updatePassword(String password){
        this.passwordHash = password;
    }

    // 회원 탈퇴
    public void delete(){
        this.isDeleted = DeleteStatus.DELETED;
    }
}
