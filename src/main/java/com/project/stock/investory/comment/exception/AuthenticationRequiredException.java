package com.project.stock.investory.comment.exception;

import com.project.stock.investory.exception.BusinessException;
import org.springframework.http.HttpStatus;

public class AuthenticationRequiredException extends BusinessException {
    public AuthenticationRequiredException() {
        super("로그인이 필요합니다.", HttpStatus.UNAUTHORIZED);
    }
}