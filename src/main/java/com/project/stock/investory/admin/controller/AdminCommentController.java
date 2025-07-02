package com.project.stock.investory.admin.controller;

import com.project.stock.investory.admin.dto.AdminCommentResponseDto;
import com.project.stock.investory.admin.dto.AdminCommentUpdateRequestDto;
import com.project.stock.investory.admin.service.AdminCommentService;
import io.swagger.v3.oas.annotations.Operation;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/admin/comments")
@RequiredArgsConstructor
public class AdminCommentController {

    private final AdminCommentService adminCommentService;

    @Operation(summary = "관리자 - 전체 댓글 목록 조회")
    @GetMapping
    @PreAuthorize("hasRole('ADMIN')")
    public List<AdminCommentResponseDto> getAllComments() {
        return adminCommentService.getAllComments();
    }

    @Operation(summary = "관리자 - 댓글 상세 조회")
    @GetMapping("/{commentId}")
    @PreAuthorize("hasRole('ADMIN')")
    public AdminCommentResponseDto getCommentById(@PathVariable Long commentId) {
        return adminCommentService.getCommentById(commentId);
    }

    @Operation(summary = "관리자 - 댓글 삭제")
    @DeleteMapping("/{commentId}")
    @PreAuthorize("hasRole('ADMIN')")
    public void deleteComment(@PathVariable Long commentId) {
        adminCommentService.deleteComment(commentId);
    }

    @Operation(summary = "관리자 - 댓글 수정 (내용만 수정 가능)")
    @PatchMapping("/{commentId}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<AdminCommentResponseDto> updateComment(
            @PathVariable Long commentId,
            @RequestBody AdminCommentUpdateRequestDto dto
    ) {
        AdminCommentResponseDto updated = adminCommentService.updateComment(commentId, dto.getContent());
        return ResponseEntity.ok(updated);
    }
}
