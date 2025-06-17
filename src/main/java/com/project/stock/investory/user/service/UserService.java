package com.project.stock.investory.user.service;

import com.project.stock.investory.user.dto.UserRequestDto;
import com.project.stock.investory.user.dto.UserResponseDto;
import com.project.stock.investory.user.entity.User;
import com.project.stock.investory.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class UserService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

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
                .phone(request.getPhone())
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
}
