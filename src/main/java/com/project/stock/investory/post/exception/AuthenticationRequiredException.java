package com.project.stock.investory.post.exception;

import com.project.stock.investory.exception.BusinessException;
import org.springframework.http.HttpStatus;

// 인증 필요 예외
public class AuthenticationRequiredException extends BusinessException {
    public AuthenticationRequiredException() {
        super("로그인이 필요합니다.", HttpStatus.UNAUTHORIZED);
    }
}