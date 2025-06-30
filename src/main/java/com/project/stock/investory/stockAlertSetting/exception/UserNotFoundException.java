package com.project.stock.investory.stockAlertSetting.exception;

import com.project.stock.investory.exception.BusinessException;
import org.springframework.http.HttpStatus;

public class UserNotFoundException extends BusinessException {
    public UserNotFoundException () {
        super("존재하지 않는 유저입니다.", HttpStatus.NOT_FOUND);
    }
}
