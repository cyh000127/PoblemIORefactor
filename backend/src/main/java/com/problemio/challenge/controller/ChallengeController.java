package com.problemio.challenge.controller;

import com.problemio.challenge.dto.ChallengeDto;
import com.problemio.challenge.dto.ChallengeResultResponse;
import com.problemio.challenge.dto.ChallengeStartResponse;
import com.problemio.challenge.service.ChallengeService;
import com.problemio.global.auth.CustomUserDetails;
import com.problemio.submission.dto.QuizAnswerResponse;
import com.problemio.submission.dto.QuizSubmissionRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/challenges")
@RequiredArgsConstructor
public class ChallengeController {

    private final ChallengeService challengeService;

    @GetMapping
    public ResponseEntity<List<ChallengeDto>> getChallenges() {
        return ResponseEntity.ok(challengeService.getChallenges());
    }

    @GetMapping("/{id}")
    public ResponseEntity<ChallengeDto> getChallenge(@PathVariable Long id) {
        return ResponseEntity.ok(challengeService.getChallenge(id));
    }

    @PostMapping("/{id}/start")
    public ResponseEntity<ChallengeStartResponse> startChallenge(
            @PathVariable Long id,
            @AuthenticationPrincipal CustomUserDetails userDetails) {
        return ResponseEntity.ok(challengeService.startChallenge(userDetails.getUser().getId(), id));
    }

    @PostMapping("/{id}/submit")
    public ResponseEntity<QuizAnswerResponse> submitAnswer(
            @PathVariable Long id,
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @RequestBody @Valid QuizSubmissionRequest request) {
        QuizAnswerResponse response = challengeService.submitAnswer(userDetails.getUser().getId(), id, request);
        java.net.URI location = java.net.URI.create("/api/challenges/" + id + "/result");
        return ResponseEntity.created(location).body(response);
    }

    @GetMapping("/{id}/result")
    public ResponseEntity<ChallengeResultResponse> getChallengeResult(
            @PathVariable Long id,
            @AuthenticationPrincipal CustomUserDetails userDetails) {
        return ResponseEntity.ok(challengeService.getChallengeResult(userDetails.getUser().getId(), id));
    }
    @GetMapping("/{id}/leaderboard")
    public ResponseEntity<com.problemio.challenge.dto.LeaderboardResponse> getLeaderboard(
            @PathVariable Long id,
            @AuthenticationPrincipal CustomUserDetails userDetails) {
        Long userId = userDetails != null ? userDetails.getUser().getId() : null;
        return ResponseEntity.ok(challengeService.getLeaderboard(id, userId));
    }
}
