package com.project.stock.investory.post.controller;

import com.project.stock.investory.post.dto.PostDto;
import com.project.stock.investory.post.dto.PostRequestDto;
import com.project.stock.investory.post.service.PostService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
//@RequestMapping("/stock/{stockId}/community")
@RequiredArgsConstructor
public class PostController {

    private final PostService postService;
    Long userId = 111L;  // 현재 로그인한 유저의 id -> 추후 로그인 연동시 삭제 및 컨트롤러 코드 수정

    /**
     * 요청 바디: PostCreateRequest (title, content)
     * userId는 임시로 111로 지정
     */
    @PostMapping("/stock/{stockId}/community")
    public ResponseEntity<PostDto> createPost(@PathVariable String stockId,
                                              @RequestBody PostRequestDto request) {
//                                              @AuthenticationPrincipal CustomUserDetails userDetails) {
//        Long userId = userDetails.getId();  // 현재 로그인한 유저의 id

        PostDto post = postService.createPost(stockId, request, userId);
        return ResponseEntity.ok(post);
    }

    @GetMapping("/stock/{stockId}/community")
    public ResponseEntity<List<PostDto>> getPostsByStockId(@PathVariable String stockId) {
        List<PostDto> posts = postService.getPostsByStockId(stockId);
        return ResponseEntity.ok(posts);
    }

    @GetMapping("/community/posts/{postId}")
    public ResponseEntity<PostDto> getPostById(@PathVariable Long postId) {
        PostDto postDto = postService.getPostById(postId);
        return ResponseEntity.ok(postDto);
    }

    @PatchMapping("/community/posts/{postId}")
    public ResponseEntity<PostDto> updatePost(@PathVariable Long postId,
                                              @RequestBody PostRequestDto request) {
//                                              , @AuthenticationPrincipal CustomUserDetails userDetails) {
//        Long userId = userDetails.getId();  // 현재 로그인한 유저의 id
        PostDto post = postService.updatePost(postId, request, userId);
        return ResponseEntity.ok(post);
    }

    @DeleteMapping("/community/posts/{postId}")
    public ResponseEntity<Void> deletePost(@PathVariable Long postId) {
//                                           , @AuthenticationPrincipal CustomUserDetails userDetails) {
//        Long userId = userDetails.getId();  // 현재 로그인한 유저의 id
        postService.deletePost(postId, userId);
        return ResponseEntity.noContent().build();
    }
}