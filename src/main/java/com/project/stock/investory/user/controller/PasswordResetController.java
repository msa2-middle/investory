package com.project.stock.investory.user.controller;

import com.project.stock.investory.user.dto.*;
import com.project.stock.investory.user.service.PasswordResetService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/users/password-reset")
@RequiredArgsConstructor
public class PasswordResetController {

    private final PasswordResetService passwordResetService;

    // 1️⃣ 인증코드 전송 (이메일 입력)
    @PostMapping("/send-code")
    public ResponseEntity<String> sendVerificationCode(@RequestBody @Valid VerificationEmailRequestDto request) {
        passwordResetService.sendVerificationCode(request);
        return ResponseEntity.ok("인증코드가 전송되었습니다.");
    }

    // 2️⃣ 인증코드 검증
    @PostMapping("/verify-code")
    public ResponseEntity<String> verifyCode(@RequestBody @Valid VerifyCodeRequestDto request) {
        passwordResetService.verifyCode(request);
        return ResponseEntity.ok("인증이 완료되었습니다.");
    }

    // 3️⃣ 비밀번호 재설정
    @PostMapping("/reset")
    public ResponseEntity<String> resetPassword(@RequestBody @Valid PasswordResetRequestDto request) {
        passwordResetService.resetPassword(request);
        return ResponseEntity.ok("비밀번호가 변경되었습니다.");
    }
}
