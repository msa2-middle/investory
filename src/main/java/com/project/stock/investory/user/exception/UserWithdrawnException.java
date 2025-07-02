package com.project.stock.investory.user.exception;

import com.project.stock.investory.exception.BusinessException;
import org.springframework.http.HttpStatus;

public class UserWithdrawnException extends BusinessException {
    public UserWithdrawnException() {
        super("탈퇴한 사용자입니다.", HttpStatus.BAD_REQUEST);
    }
}
