package com.project.stock.investory.post.exception;

import com.project.stock.investory.exception.BusinessException;
import org.springframework.http.HttpStatus;

// 좋아요 중복 예외
public class PostLikeDuplicatedException extends BusinessException {
    public PostLikeDuplicatedException() {
        super("이미 좋아요를 누른 게시글입니다", HttpStatus.CONFLICT); // 409
    }
}
