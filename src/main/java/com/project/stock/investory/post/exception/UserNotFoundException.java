package com.project.stock.investory.post.exception;

import com.project.stock.investory.exception.BusinessException;
import org.springframework.http.HttpStatus;

public class UserNotFoundException extends BusinessException {
    public UserNotFoundException() {
        super("유저가 존재하지 않습니다.", HttpStatus.NOT_FOUND); // 404
    }
}
