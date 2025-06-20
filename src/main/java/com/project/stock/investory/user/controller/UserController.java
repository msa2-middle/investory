package com.project.stock.investory.user.controller;

import com.project.stock.investory.user.dto.*;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
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

    // 마이페이지 - 회원정보 조회
    @GetMapping("/me")
    public ResponseEntity<UserResponseDto> getMyPage() {
        // SecurityContext에 저장된 사용자 ID 꺼내기
        Long userId = (Long) SecurityContextHolder.getContext().getAuthentication().getPrincipal();

        UserResponseDto user = userService.getMyInfo(userId);
        return ResponseEntity.ok(user);
    }

    // 마이페이지 - 회원정보 수정
    @PatchMapping("/me")
    public ResponseEntity<UserResponseDto> updateMyInfo(@RequestBody @Valid UserUpdateRequestDto request) {
        Long userId = (Long) SecurityContextHolder.getContext().getAuthentication().getPrincipal();

        UserResponseDto response = userService.updateUser(userId, request);
        return ResponseEntity.ok(response);
    }

    // 마이페이지 - 비밀번호 변경
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
        userService.withdraw(userId);
        return ResponseEntity.noContent().build();
    }

    // 마이페이지 - 내가 작성한 게시글 리스트
    @GetMapping("/me/posts")
    public ResponseEntity<List<PostSimpleResponseDto>> getMyPosts() {
        Long userId = (Long) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        List<PostSimpleResponseDto> myPosts = userService.getMyPosts(userId);
        return ResponseEntity.ok(myPosts);
    }

    // 마이페이지 - 내가 좋아요한 게시글 리스트
    @GetMapping("/me/likes")
    public ResponseEntity<List<PostSimpleResponseDto>> getMyLikedPosts() {
        Long userId = (Long) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        List<PostSimpleResponseDto> likedPosts = userService.getMyLikedPosts(userId);
        return ResponseEntity.ok(likedPosts);
    }

    // 마이페이지 - 내가 작성한 댓글 리스트 조회
    @GetMapping("/me/comments")
    public ResponseEntity<List<CommentSimpleResponseDto>> getMyComments() {
        Long userId = (Long) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        List<CommentSimpleResponseDto> myComments = userService.getMyComments(userId);
        return ResponseEntity.ok(myComments);
    }

    // 마이페이지 - 내가 좋아요한 댓글 리스트 조회
    @GetMapping("/me/liked-comments")
    public ResponseEntity<List<CommentSimpleResponseDto>> getMyLikedComments() {
        Long userId = (Long) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        List<CommentSimpleResponseDto> likedComments = userService.getMyLikedComments(userId);
        return ResponseEntity.ok(likedComments);
    }
}
