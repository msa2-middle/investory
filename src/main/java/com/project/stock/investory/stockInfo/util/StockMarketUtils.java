package com.project.stock.investory.stockInfo.util;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneId;

public class StockMarketUtils {
    public static boolean isTradingHours() {
        LocalTime now = LocalTime.now(ZoneId.of("Asia/Seoul"));
        LocalTime open = LocalTime.of(9, 0);
        LocalTime close = LocalTime.of(15, 30);

        DayOfWeek day = LocalDate.now(ZoneId.of("Asia/Seoul")).getDayOfWeek();
        boolean isWeekday = day != DayOfWeek.SATURDAY && day != DayOfWeek.SUNDAY;

        return isWeekday && now.isAfter(open.minusSeconds(1)) && now.isBefore(close.plusSeconds(1));
    }
}
