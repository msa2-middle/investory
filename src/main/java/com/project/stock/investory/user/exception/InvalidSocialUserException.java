package com.project.stock.investory.user.exception;

import com.project.stock.investory.exception.BusinessException;
import org.springframework.http.HttpStatus;

public class InvalidSocialUserException extends BusinessException {
    public InvalidSocialUserException() {
        super("소셜 로그인 사용자는 일반 로그인할 수 없습니다.", HttpStatus.BAD_REQUEST);
    }
}