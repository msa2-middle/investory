package com.project.stock.investory.user.exception;

import com.project.stock.investory.exception.BusinessException;
import org.springframework.http.HttpStatus;

public class SamePasswordException extends BusinessException {

    public SamePasswordException() {
        super("새 비밀번호가 기존 비밀번호와 동일합니다.", HttpStatus.BAD_REQUEST);
    }
}
