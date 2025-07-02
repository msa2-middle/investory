package com.project.stock.investory.post.exception;

import com.project.stock.investory.exception.BusinessException;
import org.springframework.http.HttpStatus;

// 게시글 접근 권한 없음 예외
public class PostAccessDeniedException extends BusinessException {
    public PostAccessDeniedException() {
        super("작성자만 수정/삭제할 수 있습니다.", HttpStatus.FORBIDDEN); // 403
    }
}