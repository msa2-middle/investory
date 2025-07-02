package com.project.stock.investory.stockInfo.exception;

import com.project.stock.investory.exception.BusinessException;
import org.springframework.http.HttpStatus;

public class StockNotFoundException extends BusinessException {

    public StockNotFoundException(String stockId) {
        super("해당 종목 ID(" + stockId+")에 대한 정보가 없습니다.", HttpStatus.NOT_FOUND );
    }
}
