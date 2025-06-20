package com.project.stock.investory.user.service;

import com.project.stock.investory.user.dto.*;
import com.project.stock.investory.user.entity.User;
import com.project.stock.investory.user.exception.CannotResetSocialUserPasswordException;
import com.project.stock.investory.user.exception.EmailVerificationException;
import com.project.stock.investory.user.exception.UserNotFoundException;
import com.project.stock.investory.user.repository.UserRepository;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.Map;
import java.util.Random;
import java.util.concurrent.ConcurrentHashMap;

@Service
@RequiredArgsConstructor
public class PasswordResetService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    // 메모리 임시 저장소 (이메일 - 인증코드)
    private Map<String, String> verificationCodes;

    @PostConstruct
    public void init() {
        verificationCodes = new ConcurrentHashMap<>();
    }

    // 1️⃣ 이메일로 인증코드 전송 (임시로 콘솔에 출력)
    public void sendVerificationCode(VerificationEmailRequestDto request) {
        // 해당 이메일로 가입한 사용자가 있는지 확인
        User user = userRepository.findByEmail(request.getEmail())
                .orElseThrow(UserNotFoundException::new);

        // 소셜 로그인 사용자는 비밀번호 재설정 불가
        if (user.getIsSocial() == 1) {
            throw new CannotResetSocialUserPasswordException();
        }

        // 랜덤 6자리 인증코드 생성
        String code = generateRandomCode();

        // 메모리에 저장 (임시)
        verificationCodes.put(request.getEmail(), code);

        // TODO: 실제 이메일 전송 로직 구현 (지금은 콘솔 출력)
        System.out.println("발송된 인증코드 (임시): " + code);
    }

    // 2️⃣ 인증코드 검증
    public void verifyCode(VerifyCodeRequestDto request) {
        String storedCode = verificationCodes.get(request.getEmail());

        if (storedCode == null || !storedCode.equals(request.getCode())) {
            throw new EmailVerificationException();
        }

        // 검증 성공하면 인증코드 삭제 (한번만 사용 가능)
        verificationCodes.remove(request.getEmail());
    }

    // 3️⃣ 비밀번호 재설정
    public void resetPassword(PasswordResetRequestDto request) {
        User user = userRepository.findByEmail(request.getEmail())
                .orElseThrow(UserNotFoundException::new);

        // 새 비밀번호 암호화 후 저장
        String encoded = passwordEncoder.encode(request.getNewPassword());
        user.changePassword(encoded);
    }

    // 6자리 랜덤 숫자 생성
    private String generateRandomCode() {
        Random random = new Random();
        int code = 100000 + random.nextInt(900000);
        return String.valueOf(code);
    }
}
