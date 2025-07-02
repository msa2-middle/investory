package com.project.stock.investory.stockAlertSetting.exception;

import com.project.stock.investory.exception.BusinessException;
import org.springframework.http.HttpStatus;

public class StockAlertSettingNotFoundException extends BusinessException {
    public StockAlertSettingNotFoundException () {
        super("존재하지 주가 알람 설정입니다.", HttpStatus.NOT_FOUND);
    }
}