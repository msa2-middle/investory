package com.project.stock.investory.stockAlertSetting.exception;

import com.project.stock.investory.exception.BusinessException;
import org.springframework.http.HttpStatus;

public class StockAlertSettingDuplicateKeyException extends BusinessException {
    public StockAlertSettingDuplicateKeyException() {
        super("이미 해당 종목에 대한 알람이 설정되어 있습니다.", HttpStatus.CONFLICT);
    }
}