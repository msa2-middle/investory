package com.project.stock.investory.user.service;

import com.project.stock.investory.user.dto.*;
import com.project.stock.investory.user.entity.User;
import com.project.stock.investory.user.repository.UserRepository;
import com.project.stock.investory.util.JwtUtil;
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

    @Transactional
    public UserResponseDto signup(UserRequestDto request) {

        // 이메일 중복 확인
        if (userRepository.findByEmail(request.getEmail()).isPresent()) {
            throw new IllegalArgumentException("이미 사용 중인 이메일입니다.");
        }

        // 소셜 로그인 여부를 Integer(0/1)로 변환
        boolean isSocial = request.getIsSocial() != null && request.getIsSocial();
        int isSocialFlag = isSocial ? 1 : 0;

        // 일반 가입인데 비밀번호가 없을 경우 예외
        if (!isSocial && (request.getPassword() == null || request.getPassword().isBlank())) {
            throw new IllegalArgumentException("비밀번호는 필수입니다.");
        }

        // 비밀번호 암호화 (소셜 로그인일 경우 null)
        String encodedPassword = isSocial ? null : passwordEncoder.encode(request.getPassword());

        // User 엔티티 생성
        User user = User.builder()
                .email(request.getEmail())
                .password(encodedPassword)
                .name(request.getName())
                .phone(isSocial ? null : request.getPhone())
                .isSocial(isSocialFlag)
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

    @Transactional(readOnly = true)
    public UserLoginResponseDto login(UserLoginRequestDto request) {
        User user = userRepository.findByEmail(request.getEmail())
                .orElseThrow(() -> new IllegalArgumentException("가입되지 않은 이메일입니다."));

        if (user.getDeletedAt() != null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "탈퇴한 사용자입니다.");
        }


        if (user.getIsSocial() == 1) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "소셜 로그인 사용자는 일반 로그인할 수 없습니다.");
        }

        if (!passwordEncoder.matches(request.getPassword(), user.getPassword())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "비밀번호가 일치하지 않습니다.");
        }

        // ✅ JWT 발급
        String token = jwtUtil.generateToken(user.getUserId(), user.getEmail());

        return UserLoginResponseDto.builder()
                .userId(user.getUserId())
                .email(user.getEmail())
                .name(user.getName())
                .accessToken(token)
                .build();
    }

    @Transactional(readOnly = true)
    public UserResponseDto getUserById(Long userId) {
        User user = validateUserExistsOrThrow(userId);

        // 탈퇴한 사용자 접근 차단
        if (user.getDeletedAt() != null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "탈퇴한 사용자입니다.");
        }

        return UserResponseDto.builder()
                .userId(user.getUserId())
                .email(user.getEmail())
                .name(user.getName())
                .build();
    }

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

    @Transactional
    public void updatePassword(Long userId, PasswordUpdateRequestDto request) {
        User user = validateUserExistsOrThrow(userId);

        if (user.getIsSocial() == 1) {
            throw new IllegalArgumentException("소셜 로그인 사용자는 비밀번호를 변경할 수 없습니다.");
        }

        // 현재 비밀번호 검증
        if (!passwordEncoder.matches(request.getCurrentPassword(), user.getPassword())) {
            throw new IllegalArgumentException("현재 비밀번호가 일치하지 않습니다.");
        }

        // 새 비밀번호 암호화 및 저장
        String encoded = passwordEncoder.encode(request.getNewPassword());
        user.changePassword(encoded);
    }

    @Transactional
    public void withdrawUser(Long userId) {
        User user = validateUserExistsOrThrow(userId);

        if (user.getDeletedAt() != null) {
            throw new IllegalStateException("이미 탈퇴한 사용자입니다.");
        }

        user.withdraw();
    }

    /**
     * 사용자 존재 여부 및 예외 처리
     */
    private User validateUserExistsOrThrow(Long userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("사용자를 찾을 수 없습니다."));
    }


}
