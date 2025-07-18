package com.project.stock.investory.comment.controller;

import com.project.stock.investory.comment.dto.CommentRequestDTO;
import com.project.stock.investory.comment.dto.CommentResponseDTO;
import com.project.stock.investory.comment.service.CommentService;
import com.project.stock.investory.security.CustomUserDetails;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/post/{postId}/comments")
@RequiredArgsConstructor // final 또는 @NonNull 필드를 매개변수로 받는 생성자를 자동 생성해주는 기능
public class CommentController {

    private final CommentService commentService;

    // 댓글 생성
    @PostMapping("/")
    public ResponseEntity<CommentResponseDTO> create(
            @RequestBody CommentRequestDTO request,
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @PathVariable Long postId
    ) {

        CommentResponseDTO response = commentService.create(request, userDetails, postId);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    // 댓글 전체 조회
    @GetMapping("/")
    public ResponseEntity<List<CommentResponseDTO>> getPostComments(
            @PathVariable Long postId
    ) {
        List<CommentResponseDTO> response =
                commentService.getPostComments(postId);

        return ResponseEntity.ok(response);
    }

    // 댓글 상세 조회
    @GetMapping("/{commentId}")
    public ResponseEntity<CommentResponseDTO> getPostComment(
            @PathVariable Long postId,
            @PathVariable Long commentId
    ) {
        CommentResponseDTO response =
                commentService.getPostComment(postId, commentId);

        return ResponseEntity.ok(response);
    }

    // 댓글 수정
    @PutMapping("/{commentId}")
    public ResponseEntity<CommentResponseDTO> updateComment(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @PathVariable Long postId,
            @PathVariable Long commentId,
            @RequestBody CommentRequestDTO request
    ) {

        CommentResponseDTO response =
                commentService.updateComment(userDetails, postId, commentId, request);
        return ResponseEntity.ok(response);
    }

    // 댓글 삭제
    @DeleteMapping("/{commentId}")
    public ResponseEntity<CommentResponseDTO> deleteComment(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @PathVariable Long postId,
            @PathVariable Long commentId
    ) {

        CommentResponseDTO response =
                commentService.deleteComment(userDetails, postId, commentId);
        return ResponseEntity.ok(response);
    }

}