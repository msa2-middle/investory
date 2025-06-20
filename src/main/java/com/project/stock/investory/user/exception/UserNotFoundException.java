package com.project.stock.investory.user.exception;

import com.project.stock.investory.exception.BusinessException;
import org.springframework.http.HttpStatus;

public class UserNotFoundException extends BusinessException {
    public UserNotFoundException() {
        super("사용자를 찾을 수 없습니다.", HttpStatus.BAD_REQUEST);
    }
}
