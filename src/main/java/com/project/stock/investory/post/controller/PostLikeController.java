package com.project.stock.investory.post.controller;

import com.project.stock.investory.post.service.PostLikeService;
import com.project.stock.investory.security.CustomUserDetails;
import io.swagger.v3.oas.annotations.Operation;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/posts")
public class PostLikeController {

    private final PostLikeService postLikeService;
    private Long userId;

    public PostLikeController(PostLikeService postLikeService) {
        this.postLikeService = postLikeService;
    }

    // 게시글 좋아요
    @Operation(summary = "게시글 좋아요", description = "새 게시글 좋아요 등록.")
    @PostMapping("/{postId}/likes")
    public ResponseEntity<Void> likePost(@PathVariable Long postId,
                                         @AuthenticationPrincipal CustomUserDetails userDetails) {
        Long userId = userDetails.getUserId();
        postLikeService.likePost(userId, postId);
        return ResponseEntity.ok().build();
    }

    // 게시글 좋아요 취소
    @Operation(summary = "게시글 좋아요 취소", description = "게시글 좋아요 취소")
    @DeleteMapping("/{postId}/likes")
    public ResponseEntity<Void> unlikePost(@PathVariable Long postId,
                                           @AuthenticationPrincipal CustomUserDetails userDetails) {
        Long userId = userDetails.getUserId();
        postLikeService.unlikePost(userId, postId);
        return ResponseEntity.ok().build();
    }

    // 게시글 좋아요 개수 조회
    @Operation(summary = "게시글 좋아요 개수 조회")
    @GetMapping("/{postId}/likes/count")
    public ResponseEntity<Long> getLikeCount(@PathVariable Long postId) {
        long count = postLikeService.countLikes(postId);
        return ResponseEntity.ok(count);
    }

    // 특정 유저가 해당 게시글에 좋아요를 눌렀는지 확인
    @Operation(summary = "특정 유저가 해당 게시글에 좋아요를 눌렀는지 확인")
    @GetMapping("/{postId}/likes/check")
    public ResponseEntity<Boolean> hasUserLiked(@PathVariable Long postId,
                                                @AuthenticationPrincipal CustomUserDetails userDetails) {
        Long userId = userDetails.getUserId();
        boolean liked = postLikeService.hasUserLiked(userId, postId);
        return ResponseEntity.ok(liked);
    }
}
