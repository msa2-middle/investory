package com.project.stock.investory.user.exception;

import com.project.stock.investory.exception.BusinessException;
import org.springframework.http.HttpStatus;

public class WeakPasswordException extends BusinessException {
    public WeakPasswordException() {
        super("비밀번호는 8자 이상, 영문자와 숫자를 포함해야 합니다.", HttpStatus.BAD_REQUEST);
    }
}
