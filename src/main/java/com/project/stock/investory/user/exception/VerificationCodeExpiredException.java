package com.project.stock.investory.user.exception;

import com.project.stock.investory.exception.BusinessException;
import org.springframework.http.HttpStatus;

public class VerificationCodeExpiredException extends BusinessException {
    public VerificationCodeExpiredException() {
        super("인증 코드가 만료되었습니다.", HttpStatus.BAD_REQUEST);
    }
}
