package com.project.stock.investory.alarm.exception;

import com.project.stock.investory.exception.BusinessException;
import org.springframework.http.HttpStatus;

public class AlarmNotFoundException extends BusinessException {
    public AlarmNotFoundException () {
        super("알람이 존재하지 않습니다.", HttpStatus.NOT_FOUND);
    }
}