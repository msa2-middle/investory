package com.project.stock.investory.post.controller;

import com.project.stock.investory.post.service.PostLikeService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/posts")
public class PostLikeController {

    private final PostLikeService postLikeService;

    public PostLikeController(PostLikeService postLikeService) {
        this.postLikeService = postLikeService;
    }

    // 게시글 좋아요
    @PostMapping("/{postId}/likes")
    public ResponseEntity<Void> likePost(@PathVariable Long postId, @RequestParam Long userId) {
        postLikeService.likePost(userId, postId);
        return ResponseEntity.ok().build();
    }

    // 게시글 좋아요 취소
    @DeleteMapping("/{postId}/likes")
    public ResponseEntity<Void> unlikePost(@PathVariable Long postId, @RequestParam Long userId) {
        postLikeService.unlikePost(userId, postId);
        return ResponseEntity.ok().build();
    }

    // 게시글 좋아요 개수 조회
    @GetMapping("/{postId}/likes/count")
    public ResponseEntity<Long> getLikeCount(@PathVariable Long postId) {
        long count = postLikeService.countLikes(postId);
        return ResponseEntity.ok(count);
    }

    // 특정 유저가 해당 게시글에 좋아요를 눌렀는지 확인
    @GetMapping("/{postId}/likes/check")
    public ResponseEntity<Boolean> hasUserLiked(@PathVariable Long postId, @RequestParam Long userId) {
        boolean liked = postLikeService.hasUserLiked(userId, postId);
        return ResponseEntity.ok(liked);
    }
}
