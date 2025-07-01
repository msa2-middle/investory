package com.project.stock.investory.user.controller;

import com.project.stock.investory.security.CustomUserDetails;
import com.project.stock.investory.user.dto.*;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;
import com.project.stock.investory.user.service.UserService;

import java.util.List;

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

    // 토큰 재발급
    @PostMapping("/refresh")
    public ResponseEntity<TokenRefreshResponseDto> refresh(@RequestBody RefreshTokenRequestDto request) {
        TokenRefreshResponseDto response = userService.refreshToken(request.getRefreshToken());
        return ResponseEntity.ok(response);
    }


    // 마이페이지 - 회원정보 조회
    @GetMapping("/me")
    public ResponseEntity<UserResponseDto> getMyPage(@AuthenticationPrincipal CustomUserDetails userDetails) {
        UserResponseDto user = userService.getMyInfo(userDetails);
        return ResponseEntity.ok(user);
    }

    // 마이페이지 - 회원정보 수정
    @PatchMapping("/me")
    public ResponseEntity<UserResponseDto> updateMyInfo(@AuthenticationPrincipal CustomUserDetails userDetails,
                                                        @RequestBody @Valid UserUpdateRequestDto request) {
        UserResponseDto response = userService.updateUser(userDetails, request);
        return ResponseEntity.ok(response);
    }

    // 마이페이지 - 비밀번호 변경
    @PatchMapping("/password")
    public ResponseEntity<Void> updatePassword(@AuthenticationPrincipal CustomUserDetails userDetails,
                                               @RequestBody @Valid PasswordUpdateRequestDto request) {
        userService.updatePassword(userDetails, request);
        return ResponseEntity.noContent().build();
    }

    // 회원 탈퇴
    @DeleteMapping("/me")
    public ResponseEntity<Void> withdrawUser(@AuthenticationPrincipal CustomUserDetails userDetails) {
        userService.withdraw(userDetails);
        return ResponseEntity.noContent().build();
    }

    // 마이페이지 - 내가 작성한 게시글 리스트
    @GetMapping("/me/posts")
    public ResponseEntity<List<PostSimpleResponseDto>> getMyPosts(@AuthenticationPrincipal CustomUserDetails userDetails) {
        List<PostSimpleResponseDto> myPosts = userService.getMyPosts(userDetails);
        return ResponseEntity.ok(myPosts);
    }

    // 마이페이지 - 내가 좋아요한 게시글 리스트
    @GetMapping("/me/likes")
    public ResponseEntity<List<PostSimpleResponseDto>> getMyLikedPosts(@AuthenticationPrincipal CustomUserDetails userDetails) {
        List<PostSimpleResponseDto> likedPosts = userService.getMyLikedPosts(userDetails);
        return ResponseEntity.ok(likedPosts);
    }

    // 마이페이지 - 내가 작성한 댓글 리스트 조회
    @GetMapping("/me/comments")
    public ResponseEntity<List<CommentSimpleResponseDto>> getMyComments(@AuthenticationPrincipal CustomUserDetails userDetails) {
        List<CommentSimpleResponseDto> myComments = userService.getMyComments(userDetails);
        return ResponseEntity.ok(myComments);
    }

    // 마이페이지 - 내가 좋아요한 댓글 리스트 조회
    @GetMapping("/me/liked-comments")
    public ResponseEntity<List<CommentSimpleResponseDto>> getMyLikedComments(@AuthenticationPrincipal CustomUserDetails userDetails) {
        List<CommentSimpleResponseDto> likedComments = userService.getMyLikedComments(userDetails);
        return ResponseEntity.ok(likedComments);
    }
}
