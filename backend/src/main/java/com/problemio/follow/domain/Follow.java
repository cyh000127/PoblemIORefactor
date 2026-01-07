// src/main/java/com/problemio/follow/domain/Follow.java
package com.problemio.follow.domain;

import lombok.Getter;
import lombok.Setter;
import lombok.Builder;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

import java.time.LocalDateTime;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Follow {
    private Long followerId;
    private Long followingId;
    private LocalDateTime createdAt;
}
