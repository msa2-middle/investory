package com.project.stock.investory.user.exception;

import com.project.stock.investory.exception.BusinessException;
import org.springframework.http.HttpStatus;

public class DuplicateSocialEmailException extends BusinessException {
    public DuplicateSocialEmailException() {
        super("해당 이메일은 소셜 계정으로 가입되어 있습니다. 소셜 로그인을 이용해주세요.", HttpStatus.BAD_REQUEST);
    }
}