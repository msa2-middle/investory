package com.project.stock.investory.post.controller;

import com.project.stock.investory.post.dto.PostDto;
import com.project.stock.investory.post.dto.PostRequestDto;
import com.project.stock.investory.post.dto.PostWithAuthorDto;
import com.project.stock.investory.post.service.PostService;
import com.project.stock.investory.security.CustomUserDetails;
import io.swagger.v3.oas.annotations.Operation;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
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
                                              @RequestBody PostRequestDto request,
                                              @AuthenticationPrincipal CustomUserDetails userDetails) {

        PostDto post = postService.createPost(stockId, request, userDetails);
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
        // 1. view_count(조회수) 증가
        postService.getPostAndIncreaseView(postId);

        // 2. 엔티티 → DTO 변환
        PostDto postDto = postService.getPostById(postId);
        return ResponseEntity.ok(postDto);
    }


    // postId로 작성자 이름 찾기
    @Operation(summary = "postId로 작성자 이름 찾기")
    @GetMapping("/community/posts/author/{postId}")
    public ResponseEntity<PostWithAuthorDto> getPost(@PathVariable Long postId) {
        PostWithAuthorDto dto = postService.getPostWithAuthor(postId);
        return ResponseEntity.ok(dto);
    }

    // get 게시글 좋아요 개수
    @Operation(summary = "get 게시글 좋아요 개수")
    @GetMapping("/community/posts/likes/{postId}")
    public ResponseEntity<Long> getPostLikeCount(@PathVariable Long postId) {
        long count = postService.getLikeCount(postId);
        return ResponseEntity.ok(count);
    }

    // get 게시글 조회수
    @Operation(summary = "get 조회수")
    @GetMapping("/community/posts/view-count/{postId}")
    public ResponseEntity<Long> getPostViewCount(@PathVariable Long postId) {
        long count = postService.getViewCount(postId);
        return ResponseEntity.ok(count);
    }

    // 개시글 수정
    @Operation(summary = "게시글 수정 ")
    @PatchMapping("/community/posts/{postId}")
    public ResponseEntity<PostDto> updatePost(@PathVariable Long postId,
                                              @RequestBody PostRequestDto request,
                                              @AuthenticationPrincipal CustomUserDetails userDetails) {

        PostDto post = postService.updatePost(postId, request, userDetails);
        return ResponseEntity.ok(post);
    }

    // 게시글 삭제
    @Operation(summary = "게시글 삭제")
    @DeleteMapping("/community/posts/{postId}")
    public ResponseEntity<Void> deletePost(@PathVariable Long postId,
                                           @AuthenticationPrincipal CustomUserDetails userDetails) {

        postService.deletePost(postId, userDetails);
        return ResponseEntity.noContent().build();
    }
}