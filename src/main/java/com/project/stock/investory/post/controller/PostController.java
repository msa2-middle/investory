package com.project.stock.investory.post.controller;

import com.project.stock.investory.post.dto.PostDto;
import com.project.stock.investory.post.dto.PostRequestDto;
import com.project.stock.investory.post.service.PostService;
import io.swagger.v3.oas.annotations.Operation;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequiredArgsConstructor
public class PostController {

    private final PostService postService;

    /**
     * 요청 바디: PostCreateRequest (title, content)
     * userId는 임시로 111로 지정
     */
    // 게시글 생성
    @Operation(summary = "게시글 생성")
    @PostMapping("/stock/{stockId}/community")
    public ResponseEntity<PostDto> createPost(@PathVariable String stockId,
                                              @RequestBody PostRequestDto request) {
//                                              @AuthenticationPrincipal CustomUserDetails userDetails) {
//        Long userId = userDetails.getId();  // 현재 로그인한 유저의 id
        Long userId = (Long) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        PostDto post = postService.createPost(stockId, request, userId);
        return ResponseEntity.ok(post);
    }

    // stockId에 해당하는 게시글 전체 조회
    @Operation(summary = "특정 종목의 전체 게시글 조회")
    @GetMapping("/stock/{stockId}/community")
    public ResponseEntity<List<PostDto>> getPostsByStockId(@PathVariable String stockId) {
        List<PostDto> posts = postService.getPostsByStockId(stockId);
        return ResponseEntity.ok(posts);
    }

    // 개별 게시글 조회
    @Operation(summary = "개별 게시글 조회")
    @GetMapping("/community/posts/{postId}")
    public ResponseEntity<PostDto> getPostById(@PathVariable Long postId) {
        PostDto postDto = postService.getPostById(postId);
        return ResponseEntity.ok(postDto);
    }

    // 개시글 수정
    @Operation(summary = "게시글 수정 ")
    @PatchMapping("/community/posts/{postId}")
    public ResponseEntity<PostDto> updatePost(@PathVariable Long postId,
                                              @RequestBody PostRequestDto request) {
//                                              , @AuthenticationPrincipal CustomUserDetails userDetails) {
//        Long userId = userDetails.getId();  // 현재 로그인한 유저의 id
        Long userId = (Long) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        PostDto post = postService.updatePost(postId, request, userId);
        return ResponseEntity.ok(post);
    }

    // 게시글 삭제
    @Operation(summary = "게시글 삭제")
    @DeleteMapping("/community/posts/{postId}")
    public ResponseEntity<Void> deletePost(@PathVariable Long postId) {
//                                           , @AuthenticationPrincipal CustomUserDetails userDetails) {
//        Long userId = userDetails.getId();  // 현재 로그인한 유저의 id
        Long userId = (Long) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        postService.deletePost(postId, userId);
        return ResponseEntity.noContent().build();
    }
}