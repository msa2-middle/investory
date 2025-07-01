package com.project.stock.investory.user.exception;

import com.project.stock.investory.exception.BusinessException;
import org.springframework.http.HttpStatus;

public class InvalidRefreshTokenException extends BusinessException {
    public InvalidRefreshTokenException() {
        super("유효하지 않은 리프레시 토큰입니다.", HttpStatus.UNAUTHORIZED);
    }
}
