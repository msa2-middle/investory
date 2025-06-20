package com.project.stock.investory.user.service;

import com.project.stock.investory.user.dto.*;
import com.project.stock.investory.user.entity.User;
import com.project.stock.investory.user.exception.*;
import com.project.stock.investory.user.repository.UserRepository;
import com.project.stock.investory.security.jwt.JwtUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class UserService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtUtil jwtUtil;

    // 회원가입 (일반 회원가입만 처리)
    @Transactional
    public UserResponseDto signup(UserRequestDto request) {

        // 이메일 중복 확인
        if (userRepository.findByEmail(request.getEmail()).isPresent()) {
            throw new DuplicateEmailException();
        }

        // 비밀번호 암호화
        String encodedPassword = passwordEncoder.encode(request.getPassword());

        // User 엔티티 생성
        User user = User.builder()
                .email(request.getEmail())
                .password(encodedPassword)
                .name(request.getName())
                .phone(request.getPhone())
                .isSocial(0)
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();

        // DB 저장
        User savedUser = userRepository.save(user);

        // 응답 DTO 반환
        return UserResponseDto.builder()
                .userId(savedUser.getUserId())
                .email(savedUser.getEmail())
                .name(savedUser.getName())
                .build();
    }

    // 로그인
    @Transactional(readOnly = true)
    public UserLoginResponseDto login(UserLoginRequestDto request) {
        User user = userRepository.findByEmail(request.getEmail())
                .orElseThrow(UserNotFoundException::new);

        // 탈퇴한 사용자 접근 차단
        if (user.getDeletedAt() != null) {
            throw new UserWithdrawnException();
        }

        // 소셜 회원인데 비밀번호가 없는 경우 → 일반 로그인 차단
        if (user.getPassword() == null || user.getIsSocial() == 1) {
            throw new InvalidSocialUserException();
        }

        // 비밀번호 불일치
        if (!passwordEncoder.matches(request.getPassword(), user.getPassword())) {
            throw new InvalidPasswordException();
        }

        // 로그인 성공 → JWT 토큰 발급
        String token = jwtUtil.generateToken(user.getUserId(), user.getEmail());

        return UserLoginResponseDto.builder()
                .userId(user.getUserId())
                .email(user.getEmail())
                .name(user.getName())
                .accessToken(token)
                .build();
    }

    // 마이페이지 조회
    @Transactional(readOnly = true)
    public UserResponseDto getUserById(Long userId) {
        User user = validateUserExistsOrThrow(userId);

        // 탈퇴한 사용자 접근 차단
        if (user.getDeletedAt() != null) {
            throw new UserWithdrawnException();
        }

        return UserResponseDto.builder()
                .userId(user.getUserId())
                .email(user.getEmail())
                .name(user.getName())
                .build();
    }

    // 회원정보 수정
    @Transactional
    public UserResponseDto updateUser(Long userId, UserUpdateRequestDto request) {
        User user = validateUserExistsOrThrow(userId);

        user.updateInfo(request.getName(), request.getPhone());

        return UserResponseDto.builder()
                .userId(user.getUserId())
                .email(user.getEmail())
                .name(user.getName())
                .build();
    }

    // 비밀번호 변경
    @Transactional
    public void updatePassword(Long userId, PasswordUpdateRequestDto request) {
        User user = validateUserExistsOrThrow(userId);

        // 소셜 로그인 사용자는 비밀번호 변경 불가
        if (user.getPassword() == null || user.getIsSocial() == 1) {
            throw new InvalidSocialUserException();
        }

        // 현재 비밀번호 검증
        if (!passwordEncoder.matches(request.getCurrentPassword(), user.getPassword())) {
            throw new InvalidPasswordException();
        }

        // 새 비밀번호 암호화 및 저장
        String encoded = passwordEncoder.encode(request.getNewPassword());
        user.changePassword(encoded);
    }

    // 회원 탈퇴 (soft delete)
    @Transactional
    public void withdrawUser(Long userId) {
        User user = validateUserExistsOrThrow(userId);

        // 이미 탈퇴한 사용자인 경우 예외 처리
        if (user.getDeletedAt() != null) {
            throw new UserWithdrawnException();
        }

        user.withdraw();
    }

    // 공통 사용자 조회 유틸리티 메서드
    private User validateUserExistsOrThrow(Long userId) {
        return userRepository.findById(userId)
                .orElseThrow(UserNotFoundException::new);
    }



}
