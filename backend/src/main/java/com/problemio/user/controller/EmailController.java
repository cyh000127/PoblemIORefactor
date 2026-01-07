package com.problemio.user.controller;


import com.problemio.user.dto.EmailVerifyRequest;
import com.problemio.user.service.EmailService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auth/email")
@RequiredArgsConstructor
public class EmailController {

    private final EmailService emailService;

    // 1. 인증번호 전송 요청
    // GET 또는 POST /api/auth/email/send?email=abc@naver.com
    @PostMapping("/send")
    public ResponseEntity<?> sendEmail(@RequestParam String email) {
        emailService.sendVerificationCode(email);
        return ResponseEntity.ok("인증 코드가 발송되었습니다.");
    }

    // 2. 인증번호 검증 요청
    // POST /api/auth/email/verify (Body: {email, code})
    @PostMapping("/verify")
    public ResponseEntity<?> verifyEmail(@RequestBody EmailVerifyRequest request) {
        boolean isVerified = emailService.verifyCode(request.getEmail(), request.getCode());

        if (isVerified) {
            return ResponseEntity.ok("인증 성공");
        } else {
            return ResponseEntity.status(400).body("인증 실패: 코드가 틀리거나 만료되었습니다.");
        }
    }
}