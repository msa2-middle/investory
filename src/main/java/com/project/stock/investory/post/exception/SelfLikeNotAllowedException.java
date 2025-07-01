package com.project.stock.investory.post.exception;

import com.project.stock.investory.exception.BusinessException;
import org.springframework.http.HttpStatus;

// 좋아요 중복 예외
public class SelfLikeNotAllowedException extends BusinessException {
    public SelfLikeNotAllowedException() {
        super("자신의 글에는 좋아요를 누를 수 없습니다", HttpStatus.CONFLICT);
    }
}
