package com.stock.project.investory.user.service;

import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.stock.project.investory.user.dto.UserRequestDto;
import com.stock.project.investory.user.dto.UserResponseDto;
import com.stock.project.investory.user.entity.User;
import com.stock.project.investory.user.repository.UserRepository;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class UserService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    @Transactional
    public UserResponseDto signup(UserRequestDto request) {

        // 이메일 중복 체크
        if (userRepository.findByEmail(request.getEmail()).isPresent()) {
            throw new IllegalArgumentException("이미 사용 중인 이메일입니다.");
        }

        // 비밀번호 암호화 (소셜 로그인인 경우 null 유지)
        String encryptedPassword = request.getIsSocial() ? null :
                passwordEncoder.encode(request.getPassword());

        // User 엔티티 생성
        User user = User.builder()
                .email(request.getEmail())
                .password(encryptedPassword)
                .name(request.getName())
                .phone(request.getPhone())
                .isSocial(request.getIsSocial())
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();

        // 저장
        User savedUser = userRepository.save(user);

        // 응답 DTO 반환
        return UserResponseDto.builder()
                .userId(savedUser.getUserId())
                .email(savedUser.getEmail())
                .name(savedUser.getName())
                .build();
    }
}
