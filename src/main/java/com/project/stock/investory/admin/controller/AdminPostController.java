package com.project.stock.investory.admin.controller;

import com.project.stock.investory.admin.dto.AdminPostResponseDto;
import com.project.stock.investory.post.dto.PostRequestDto;
import com.project.stock.investory.post.dto.PostWithAuthorDto;
import com.project.stock.investory.admin.service.AdminPostService;
import io.swagger.v3.oas.annotations.Operation;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/admin/posts")
public class AdminPostController {

    private final AdminPostService adminPostService;

    @Operation(summary = "관리자 - 전체 게시글 목록 조회")
    @PreAuthorize("hasRole('ADMIN')")
    @GetMapping
    public ResponseEntity<List<AdminPostResponseDto>> getAllPosts() {
        List<AdminPostResponseDto> posts = adminPostService.getAllPosts();
        return ResponseEntity.ok(posts);
    }

    @Operation(summary = "관리자 - 게시글 상세 조회")
    @PreAuthorize("hasRole('ADMIN')")
    @GetMapping("/{postId}")
    public ResponseEntity<PostWithAuthorDto> getPostById(@PathVariable Long postId) {
        PostWithAuthorDto post = adminPostService.getPostById(postId);
        return ResponseEntity.ok(post);
    }

    @Operation(summary = "관리자 - 게시글 삭제")
    @PreAuthorize("hasRole('ADMIN')")
    @DeleteMapping("/{postId}")
    public ResponseEntity<Void> deletePostById(@PathVariable Long postId) {
        adminPostService.deletePostById(postId);
        return ResponseEntity.noContent().build();
    }

    @Operation(summary = "관리자 - 게시글 수정 (제목, 내용만 수정)")
    @PreAuthorize("hasRole('ADMIN')")
    @PatchMapping("/{postId}")
    public ResponseEntity<PostWithAuthorDto> updatePostById(
            @PathVariable Long postId,
            @RequestBody PostRequestDto request) {

        PostWithAuthorDto updatedPost = adminPostService.updatePostById(
                postId,
                request.getTitle(),
                request.getContent()
        );
        return ResponseEntity.ok(updatedPost);
    }

}
