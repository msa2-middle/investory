package com.project.stock.investory.comment.exception;

import com.project.stock.investory.exception.BusinessException;
import org.springframework.http.HttpStatus;

public class CommentAccessDeniedException extends BusinessException {
    public CommentAccessDeniedException() {
        super("작성자만 수정/삭제할 수 있습니다.", HttpStatus.FORBIDDEN); // 403
    }
}