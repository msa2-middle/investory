package com.project.stock.investory.user.exception;

import com.project.stock.investory.exception.BusinessException;
import org.springframework.http.HttpStatus;

public class InvalidRoleException extends BusinessException {
    public InvalidRoleException() {
        super("잘못된 권한 값입니다.", HttpStatus.BAD_REQUEST);
    }
}
