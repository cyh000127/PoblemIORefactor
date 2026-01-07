package com.problemio.ranking.dto;

import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.problemio.global.config.S3UrlSerializer;

public class RankingResponseDto {
    private Long userId;
    private String nickname;


    @JsonSerialize(using = S3UrlSerializer.class)
    private String profileImageUrl;

    private int solvedQuizCount;
    private double accuracy;
    private int score;

    public static RankingResponseDto of(RankingRowDto row, int score) {
        RankingResponseDto dto = new RankingResponseDto();
        dto.userId = row.getUserId();
        dto.nickname = row.getNickname();
        dto.profileImageUrl = row.getProfileImageUrl();
        dto.solvedQuizCount = row.getSolvedQuizCount();
        dto.accuracy = row.getAccuracy();
        dto.score = score;
        return dto;
    }

    public Long getUserId() {
        return userId;
    }

    public void setUserId(Long userId) {
        this.userId = userId;
    }

    public String getNickname() {
        return nickname;
    }

    public void setNickname(String nickname) {
        this.nickname = nickname;
    }

    public String getProfileImageUrl() {
        return profileImageUrl;
    }

    public void setProfileImageUrl(String profileImageUrl) {
        this.profileImageUrl = profileImageUrl;
    }

    public int getSolvedQuizCount() {
        return solvedQuizCount;
    }

    public void setSolvedQuizCount(int solvedQuizCount) {
        this.solvedQuizCount = solvedQuizCount;
    }

    public double getAccuracy() {
        return accuracy;
    }

    public void setAccuracy(double accuracy) {
        this.accuracy = accuracy;
    }

    public int getScore() {
        return score;
    }

    public void setScore(int score) {
        this.score = score;
    }
}
