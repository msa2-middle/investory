package com.project.stock.investory.user.exception;

import com.project.stock.investory.exception.BusinessException;
import org.springframework.http.HttpStatus;

public class UserEmailNotFoundException extends BusinessException {
    public UserEmailNotFoundException() {
        super("해당 이메일을 가진 사용자를 찾을 수 없습니다.", HttpStatus.BAD_REQUEST);
    }
}
