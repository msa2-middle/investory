package com.project.stock.investory.user.service;

import com.project.stock.investory.comment.model.Comment;
import com.project.stock.investory.comment.repository.CommentRepository;
import com.project.stock.investory.commentLike.model.CommentLike;
import com.project.stock.investory.commentLike.repository.CommentLikeRepository;
import com.project.stock.investory.post.entity.Post;
import com.project.stock.investory.post.entity.PostLike;
import com.project.stock.investory.post.repository.PostLikeRepository;
import com.project.stock.investory.post.repository.PostRepository;
import com.project.stock.investory.security.CustomUserDetails;
import com.project.stock.investory.stockAlertSetting.processor.StockPriceProcessor;
import com.project.stock.investory.user.dto.*;
import com.project.stock.investory.user.entity.User;
import com.project.stock.investory.user.exception.*;
import com.project.stock.investory.user.repository.UserRepository;
import com.project.stock.investory.security.jwt.JwtUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.regex.Pattern;

@Service
@RequiredArgsConstructor
public class UserService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtUtil jwtUtil;
    private final PostRepository postRepository;
    private final PostLikeRepository postLikeRepository;
    private final CommentRepository commentRepository;
    private final CommentLikeRepository commentLikeRepository;
    private final StockPriceProcessor stockPriceProcessor;
    private static final Pattern LETTER_PATTERN = Pattern.compile("[a-zA-Z]");
    private static final Pattern DIGIT_PATTERN = Pattern.compile("\\d");
    private static final Pattern SPECIAL_PATTERN = Pattern.compile("[!@#$%^&*()_+\\-=\\[\\]{};':\"\\\\|,.<>\\/?]");



    private void validatePasswordStrength(String password) {
        int score = 0;

        // 길이가 8자 이상이면 +1
        if (password.length() >= 8) score++;

        // 영문자(a~z 또는 A~Z)가 있으면 +1
        if (LETTER_PATTERN.matcher(password).find()) score++;

        // 숫자(0~9)가 있으면 +1
        if (DIGIT_PATTERN.matcher(password).find()) score++;

        // 특수문자(예: !, @, # 등)가 있으면 +1
        if (SPECIAL_PATTERN.matcher(password).find()) score++;

        if (score < 3) { // 기본 조건 (길이 + 영문자 + 숫자 만족해야 통과)
            throw new WeakPasswordException();
        }
    }
    // 회원가입 (일반 회원가입만 처리)
    @Transactional
    public UserResponseDto signup(UserRequestDto request) {

        // 이메일 중복 확인
        userRepository.findByEmail(request.getEmail()).ifPresent(existingUser -> {
            if (existingUser.getIsSocial() == 1) {
                // 이미 소셜 계정으로 가입된 경우
                throw new DuplicateSocialEmailException();
            } else {
                throw new DuplicateEmailException();
            }
        });

        // 비밀번호 복잡도 검사
        validatePasswordStrength(request.getPassword());

        // 비밀번호 암호화
        String encodedPassword = passwordEncoder.encode(request.getPassword());

        // User 엔티티 생성
        User user = User.builder()
                .email(request.getEmail())
                .password(encodedPassword)
                .name(request.getName())
                .phone(request.getPhone())
                .isSocial(0)
                .build();

        // DB 저장
        User savedUser = userRepository.save(user);

        // 캐시 등록
        stockPriceProcessor.updateUserCache(savedUser);

        // 응답 DTO 반환
        return UserResponseDto.builder()
                .userId(savedUser.getUserId())
                .email(savedUser.getEmail())
                .name(savedUser.getName())
                .build();
    }

    // 로그인
    @Transactional
    public UserLoginResponseDto login(UserLoginRequestDto request) {
        User user = userRepository.findByEmail(request.getEmail())
                .orElseThrow(UserNotFoundException::new);

        validateUserActive(user);

        // 소셜로만 가입했고, 비밀번호가 없는 경우에만 일반 로그인 차단
        if (user.getPassword() == null) {
            throw new InvalidSocialUserException();
        }

        // 비밀번호 불일치
        if (!passwordEncoder.matches(request.getPassword(), user.getPassword())) {
            throw new InvalidPasswordException();
        }

        // AccessToken 생성
        String accessToken  = jwtUtil.generateToken(user.getUserId(), user.getEmail(), user.getName());

        // RefreshToken 생성
        String refreshToken = jwtUtil.generateRefreshToken(user.getUserId(), user.getEmail(), user.getName());

        user.updateRefreshToken(refreshToken);
        userRepository.save(user);

        return UserLoginResponseDto.builder()
                .userId(user.getUserId())
                .email(user.getEmail())
                .name(user.getName())
                .accessToken(accessToken)
                .refreshToken(refreshToken)
                .build();
    }

    @Transactional
    public TokenRefreshResponseDto refreshToken(String refreshToken) {
        if (!jwtUtil.validateRefreshToken(refreshToken)) {
            throw new InvalidRefreshTokenException();
        }

        Long userId = jwtUtil.getUserIdFromToken(refreshToken);
        User user = userRepository.findById(userId)
                .orElseThrow(UserNotFoundException::new);

        // DB에 저장된 refreshToken과 비교
        if (!refreshToken.equals(user.getRefreshToken())) {
            throw new InvalidRefreshTokenException();
        }

        String newAccessToken = jwtUtil.generateToken(user.getUserId(), user.getEmail(), user.getName());
        String newRefreshToken = jwtUtil.generateRefreshToken(user.getUserId(), user.getEmail(), user.getName());

        user.updateRefreshToken(newRefreshToken);

        return TokenRefreshResponseDto.builder()
                .accessToken(newAccessToken)
                .refreshToken(newRefreshToken)
                .build();
    }


    // 내 정보 조회 (마이페이지 조회)
    @Transactional(readOnly = true)
    public UserResponseDto getMyInfo(CustomUserDetails userDetails) {
        User user = findActiveUser(userDetails.getUserId());

        return UserResponseDto.builder()
                .userId(user.getUserId())
                .email(user.getEmail())
                .name(user.getName())
                .phone(user.getPhone())
                .build();
    }

    // 내 정보 수정 (마이페이지 수정)
    @Transactional
    public UserResponseDto updateUser(CustomUserDetails userDetails, UserUpdateRequestDto request) {
        User user = findActiveUser(userDetails.getUserId());

        // null 방어: 전화번호가 null로 넘어오면 기존 전화번호 유지
        String phoneToUpdate = request.getPhone() != null ? request.getPhone() : user.getPhone();

        user.updateInfo(request.getName(), phoneToUpdate);

        // 캐시 갱신
        stockPriceProcessor.updateUserCache(user);

        return UserResponseDto.builder()
                .userId(user.getUserId())
                .email(user.getEmail())
                .name(user.getName())
                .build();
    }

    // 비밀번호 변경 (마이페이지에서 변경)
    @Transactional
    public void updatePassword(CustomUserDetails userDetails, PasswordUpdateRequestDto request) {
        User user = findActiveUser(userDetails.getUserId());

        // 비밀번호가 없는 소셜-only 유저는 비밀번호 변경 불가
        if (user.getPassword() == null) {
            throw new InvalidSocialUserException();
        }

        // 현재 비밀번호 검증
        if (!passwordEncoder.matches(request.getCurrentPassword(), user.getPassword())) {
            throw new InvalidPasswordException();
        }

        // 새 비밀번호가 기존 비밀번호와 동일한지 검사
        if (passwordEncoder.matches(request.getNewPassword(), user.getPassword())) {
            throw new SamePasswordException();
        }

        // 새 비밀번호 복잡도 검사
        validatePasswordStrength(request.getNewPassword());

        // 새 비밀번호 암호화 및 저장
        String encoded = passwordEncoder.encode(request.getNewPassword());
        user.changePassword(encoded);
    }

    // 회원 탈퇴 (soft delete)
    @Transactional
    public void withdraw(CustomUserDetails userDetails) {
        User user = findActiveUser(userDetails.getUserId());

        // 이미 탈퇴한 사용자인 경우 예외 처리
        if (user.getDeletedAt() != null) {
            throw new UserWithdrawnException();
        }

        user.withdraw();

        // 캐시 제거
        stockPriceProcessor.removeUserCache(user.getUserId());
    }

    // 내가 작성한 게시글 목록 조회
    @Transactional(readOnly = true)
    public List<PostSimpleResponseDto> getMyPosts(CustomUserDetails userDetails) {
        List<Post> posts = postRepository.findByUserId(userDetails.getUserId());

        return posts.stream()
                .map(post -> PostSimpleResponseDto.builder()
                        .postId(post.getPostId())
                        .title(post.getTitle())
                        .createdAt(post.getCreatedAt())
                        .build())
                .toList();
    }

    // 내가 좋아요한 게시글 목록 조회
    @Transactional(readOnly = true)
    public List<PostSimpleResponseDto> getMyLikedPosts(CustomUserDetails userDetails) {
        List<PostLike> likes = postLikeRepository.findByUser_UserId(userDetails.getUserId());

        return likes.stream()
                .map(like -> PostSimpleResponseDto.builder()
                        .postId(like.getPost().getPostId())
                        .title(like.getPost().getTitle())
                        .createdAt(like.getCreatedAt())
                        .build())
                .toList();
    }

    // 내가 작성한 댓글 목록 조회
    @Transactional(readOnly = true)
    public List<CommentSimpleResponseDto> getMyComments(CustomUserDetails userDetails) {
        List<Comment> comments = commentRepository.findByUser_UserId(userDetails.getUserId());

        return comments.stream()
                .map(comment -> CommentSimpleResponseDto.builder()
                        .commentId(comment.getCommentId())
                        .content(comment.getContent())
                        .createdAt(comment.getCreatedAt())
                        .build())
                .toList();
    }

    // 내가 좋아요한 댓글 조회
    @Transactional(readOnly = true)
    public List<CommentSimpleResponseDto> getMyLikedComments(CustomUserDetails userDetails) {
        List<CommentLike> likes = commentLikeRepository.findByUser_UserId(userDetails.getUserId());

        return likes.stream()
                .map(like -> CommentSimpleResponseDto.builder()
                        .commentId(like.getComment().getCommentId())
                        .content(like.getComment().getContent())
                        .createdAt(like.getComment().getCreatedAt())
                        .build())
                .toList();
    }

    // ====== 내부 유틸 메서드 ======

    private User findActiveUser(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(UserNotFoundException::new);
        validateUserActive(user);
        return user;
    }

    private void validateUserActive(User user) {
        if (user.getDeletedAt() != null) {
            throw new UserWithdrawnException();
        }
    }
}
