package com.project.stock.investory.user.exception;

import com.project.stock.investory.exception.BusinessException;
import org.springframework.http.HttpStatus;

public class CannotResetSocialUserPasswordException extends BusinessException {
    public CannotResetSocialUserPasswordException() {
        super("소셜 로그인 사용자는 비밀번호 재설정을 지원하지 않습니다.", HttpStatus.BAD_REQUEST);
    }
}
