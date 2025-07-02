package com.project.stock.investory.commentLike.controller;

import com.project.stock.investory.commentLike.dto.CommentLikeResponseDTO;
import com.project.stock.investory.commentLike.service.CommentLikeService;
import com.project.stock.investory.security.CustomUserDetails;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
@RequestMapping("/comments/{commentId}")
public class CommentLikeController {

    private final CommentLikeService commentLikeService;

    // 댓글 좋아요 토글 (좋아요/좋아요 취소)
    @PostMapping("/like")
    public ResponseEntity<CommentLikeResponseDTO> toggleCommentLike(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @PathVariable Long commentId) {

      CommentLikeResponseDTO response =
                commentLikeService.toggleCommentLike(userDetails, commentId);

        return ResponseEntity.ok(response);
    }

    // 사용자의 댓글 좋아요 상태 확인
    @GetMapping("/like/status")
    public ResponseEntity<Boolean> checkLikeStatus(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @PathVariable Long commentId) {

        boolean isLiked = commentLikeService.hasUserLikedComment(userDetails, commentId);
        return ResponseEntity.ok(isLiked);
    }

    // 댓글의 총 좋아요 수 조회
    @GetMapping("/like/count")
    public ResponseEntity<Integer> getLikeCount(@PathVariable Long commentId) {
        int likeCount = commentLikeService.getCommentLikeCount(commentId);
        return ResponseEntity.ok(likeCount);
    }
}