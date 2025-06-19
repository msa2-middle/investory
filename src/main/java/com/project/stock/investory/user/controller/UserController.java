package com.project.stock.investory.user.controller;

import com.project.stock.investory.user.dto.*;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;
import com.project.stock.investory.user.service.UserService;

@RestController
@RequestMapping("/users")
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;

    // 회원가입
    @PostMapping("/signup")
    public ResponseEntity<UserResponseDto> signup(@Valid @RequestBody UserRequestDto request) {
        UserResponseDto response = userService.signup(request);
        return ResponseEntity.ok(response);
    }

    // 로그인
    @PostMapping("/login")
    public ResponseEntity<UserLoginResponseDto> login(@Valid @RequestBody UserLoginRequestDto request) {
        UserLoginResponseDto response = userService.login(request);
        return ResponseEntity.ok(response);
    }

    // 마이페이지 조회
    @GetMapping("/me")
    public ResponseEntity<UserResponseDto> getMyPage() {
        // SecurityContext에 저장된 사용자 ID 꺼내기
        Long userId = (Long) SecurityContextHolder.getContext().getAuthentication().getPrincipal();

        UserResponseDto user = userService.getUserById(userId);
        return ResponseEntity.ok(user);
    }

    // 회원정보 수정
    @PatchMapping("/me")
    public ResponseEntity<UserResponseDto> updateMyInfo(@RequestBody @Valid UserUpdateRequestDto request) {
        Long userId = (Long) SecurityContextHolder.getContext().getAuthentication().getPrincipal();

        UserResponseDto response = userService.updateUser(userId, request);
        return ResponseEntity.ok(response);
    }

    // 비밀번호 변경
    @PatchMapping("/password")
    public ResponseEntity<Void> updatePassword(@RequestBody @Valid PasswordUpdateRequestDto request) {
        Long userId = (Long) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        userService.updatePassword(userId, request);
        return ResponseEntity.noContent().build();
    }

    // 회원 탈퇴
    @DeleteMapping("/me")
    public ResponseEntity<Void> withdrawUser() {
        Long userId = (Long) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        userService.withdrawUser(userId);
        return ResponseEntity.noContent().build();
    }


}
