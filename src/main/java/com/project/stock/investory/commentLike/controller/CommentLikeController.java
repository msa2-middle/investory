package com.project.stock.investory.commentLike.controller;

import com.project.stock.investory.commentLike.service.CommentLikeService;
import com.project.stock.investory.security.CustomUserDetails;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;


@RestController
@RequiredArgsConstructor
@RequestMapping("/comments/{commentId}/comment-like")
public class CommentLikeController {

    private final CommentLikeService commentLikeService;

    // 댓글 좋아요 설정
    @PostMapping("/")
    public void addCommentLike(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @PathVariable Long commentId) {

        commentLikeService.addCommentLike(userDetails, commentId);

    }

}
