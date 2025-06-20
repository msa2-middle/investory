package com.project.stock.investory.user.exception;

import com.project.stock.investory.exception.BusinessException;
import org.springframework.http.HttpStatus;

public class InvalidPasswordException extends BusinessException {
    public InvalidPasswordException() {
        super("비밀번호가 일치하지 않습니다.", HttpStatus.BAD_REQUEST);
    }
}