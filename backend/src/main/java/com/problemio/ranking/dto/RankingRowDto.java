package com.problemio.ranking.dto;

import java.time.LocalDateTime;

public class RankingRowDto {
    private Long userId;
    private String nickname;
    private String profileImageUrl;

    private int solvedQuizCount;
    private int totalCorrect;
    private int totalQuestions;
    private double accuracy;
    private LocalDateTime lastSubmittedAt;

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

    public int getTotalCorrect() {
        return totalCorrect;
    }

    public void setTotalCorrect(int totalCorrect) {
        this.totalCorrect = totalCorrect;
    }

    public int getTotalQuestions() {
        return totalQuestions;
    }

    public void setTotalQuestions(int totalQuestions) {
        this.totalQuestions = totalQuestions;
    }

    public double getAccuracy() {
        return accuracy;
    }

    public void setAccuracy(double accuracy) {
        this.accuracy = accuracy;
    }

    public LocalDateTime getLastSubmittedAt() {
        return lastSubmittedAt;
    }

    public void setLastSubmittedAt(LocalDateTime lastSubmittedAt) {
        this.lastSubmittedAt = lastSubmittedAt;
    }
}
