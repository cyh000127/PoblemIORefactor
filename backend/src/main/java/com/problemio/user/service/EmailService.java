package com.problemio.user.service;

import com.problemio.global.exception.BusinessException;
import com.problemio.global.exception.ErrorCode;
import com.problemio.user.mapper.UserAuthMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Service
@RequiredArgsConstructor
public class EmailService {

    private final JavaMailSender mailSender;
    private final UserAuthMapper userAuthMapper;

    // DB 대신 메모리에 저장 (Key: 이메일, Value: 인증정보)
    private final Map<String, VerificationInfo> memoryStore = new ConcurrentHashMap<>();
    
    // 인증 완료된 이메일 저장 (Key: 이메일, Value: 만료시간)
    private final Map<String, Long> verifiedEmails = new ConcurrentHashMap<>();

    // 내부 클래스: 인증번호와 만료시간을 담는 객체
    private static class VerificationInfo {
        String code;
        long expireTime;

        public VerificationInfo(String code, long expireTime) {
            this.code = code;
            this.expireTime = expireTime;
        }
    }

    // 1. 인증 코드 발송 및 저장
    public void sendVerificationCode(String email) {
        // 이미 가입된 이메일인지 확인
        if (userAuthMapper.findByEmail(email).isPresent()) {
            throw new BusinessException(ErrorCode.EMAIL_DUPLICATED);
        }

        // 6자리 난수 생성
        String code = String.valueOf((int)(Math.random() * 900000) + 100000);

        // 이메일 전송 객체 생성
        SimpleMailMessage message = new SimpleMailMessage();
        message.setTo(email);
        message.setSubject("[ProblemIO] 이메일 인증 번호입니다.");
        message.setText("인증 번호: " + code + "\n\n3분 이내에 입력해주세요.");

        // mailSender.send(message);
        System.out.println(">>> [Local Dev] 이메일 발송 우회 - 인증 번호: " + code);

        // 메모리에 저장 (유효시간 3분 = 180000ms)
        long expireTime = System.currentTimeMillis() + (3 * 60 * 1000);
        memoryStore.put(email, new VerificationInfo(code, expireTime));
    }

    // 2. 인증 코드 검증
    public boolean verifyCode(String email, String code) {
        VerificationInfo info = memoryStore.get(email);

        // 기록이 없거나, 코드가 틀리거나, 시간이 만료되었으면 false
        if (info == null || !info.code.equals(code) || System.currentTimeMillis() > info.expireTime) {
            return false;
        }

        // 검증 성공 시 메모리에서 삭제 (재사용 방지) 후 인증 완료 목록에 추가 (10분 유효)
        memoryStore.remove(email);
        verifiedEmails.put(email, System.currentTimeMillis() + (10 * 60 * 1000));
        return true;
    }

    // 3. 이메일 인증 여부 확인
    public boolean isEmailVerified(String email) {
        Long expireTime = verifiedEmails.get(email);
        return expireTime != null && expireTime > System.currentTimeMillis();
    }

    // 4. 인증 사용 완료 처리 (가입 완료 시 호출)
    public void consumeVerification(String email) {
        verifiedEmails.remove(email);
    }

    // 1시간마다 만료된 데이터 청소 (메모리 누수 방지)
    @Scheduled(fixedRate = 3600000)
    public void clearExpiredData() {
        long now = System.currentTimeMillis();
        memoryStore.entrySet().removeIf(entry -> entry.getValue().expireTime < now);
        verifiedEmails.entrySet().removeIf(entry -> entry.getValue() < now);
    }
}