package com.project.stock.investory.post.exception;

import com.project.stock.investory.exception.BusinessException;
import org.springframework.http.HttpStatus;

public class PostNotFoundException extends BusinessException {
    public PostNotFoundException() {
        super("게시글을 찾을 수 없습니다.", HttpStatus.NOT_FOUND); // 404
    }
}
