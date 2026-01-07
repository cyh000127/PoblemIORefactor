package com.problemio.user.domain;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter
@Setter
@Builder
public class RefreshToken {
    private Long id;
    private Long userId;
    private String tokenValue;
    private LocalDateTime expiresAt;
    private LocalDateTime createdAt;
}