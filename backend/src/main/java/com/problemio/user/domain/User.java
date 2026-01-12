package com.problemio.user.domain;

import jakarta.persistence.*;
import lombok.*;
import org.springframework.boot.context.properties.bind.DefaultValue;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;

import com.problemio.user.domain.Role;


import java.time.LocalDateTime;

import org.springframework.data.jpa.domain.support.AuditingEntityListener;

@Getter
@Entity
@Table(name="users")
@EntityListeners(AuditingEntityListener.class)
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

    @Column
    private String profileTheme;

    @Column
    private String avatarDecoration;

    @Column
    private String popoverDecoration;

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

    public boolean getStatus() {
       return this.isDeleted == DeleteStatus.ACTIVE;
    }

    // == 비즈니스 로직 ==//
    // 프로필 업데이트
    public void updateProfile(String nickname, String statusMessage, String profileImageUrl){
        if(nickname != null) this.nickname = nickname;
        if(statusMessage != null) this.statusMessage = statusMessage;
        if(profileImageUrl != null) this.profileImageUrl = profileImageUrl;
    }

    public void updateDecorations(String profileTheme, String avatarDecoration, String popoverDecoration) {
        if (profileTheme != null) this.profileTheme = profileTheme;
        if (avatarDecoration != null) this.avatarDecoration = avatarDecoration;
        if (popoverDecoration != null) this.popoverDecoration = popoverDecoration;
    }

    // 비밀번호 변경
    public void updatePassword(String password){
        this.passwordHash = password;
    }

    // 회원 탈퇴
    public void delete(){
        this.isDeleted = DeleteStatus.DELETED;
        // 탈퇴 시 이메일/비밀번호 랜덤화 등을 여기서 수행할 수도 있음
        // 유연성을 위해 별도 anonymize 메서드로 분리함
    }

    public void anonymize(String email, String password) {
        this.email = email;
        this.passwordHash = password;
        // 닉네임 등 다른 필드도 필요 시 익명화
    }
}
