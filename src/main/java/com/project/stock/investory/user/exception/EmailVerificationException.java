package com.project.stock.investory.user.exception;

import com.project.stock.investory.exception.BusinessException;
import org.springframework.http.HttpStatus;

public class EmailVerificationException extends BusinessException {
    public EmailVerificationException() {
        super("인증 코드가 일치하지 않습니다.", HttpStatus.BAD_REQUEST);
    }
}
