package com.problemio.user.domain;

import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter
@Setter
public class User {

    private Long id;
    private String email;
    private String passwordHash;
    private String nickname;
    private String profileImageUrl;
    private String statusMessage;
    private boolean isDeleted;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private String role; // ROLE_USER, ROLE_ADMIN
}
