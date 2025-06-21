package com.project.stock.investory.alarm.websocket;

import com.project.stock.investory.alarm.dto.AlarmRequestDTO;
import com.project.stock.investory.alarm.entity.AlarmType;
import com.project.stock.investory.alarm.service.AlarmService;
import com.project.stock.investory.stockAlertSetting.model.ConditionType;
import com.project.stock.investory.stockAlertSetting.model.StockAlertSetting;
import com.project.stock.investory.stockInfo.model.Stock;
import com.project.stock.investory.stockInfo.repository.StockRepository;
import com.project.stock.investory.user.entity.User;
import com.project.stock.investory.user.repository.UserRepository;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

@Component
@RequiredArgsConstructor
public class StockPriceProcessor {

    private final AlarmService alarmService;
    private final UserRepository userRepository;
    private final StockRepository stockRepository;
    private final Map<String, NavigableMap<Integer, List<AlertCondition>>> overMap = new ConcurrentHashMap<>();
    private final Map<String, NavigableMap<Integer, List<AlertCondition>>> underMap = new ConcurrentHashMap<>();

    public void loadConditions(List<StockAlertSetting> settings) {
        for (StockAlertSetting setting : settings) {
            AlertCondition condition = new AlertCondition(
                    setting.getSettingId(),
                    setting.getUser().getUserId(),
                    setting.getStock().getStockId(),
                    setting.getTargetPrice(),
                    setting.getCondition()
            );

            if (condition.getCondition() == ConditionType.ABOVE) {
                overMap.computeIfAbsent(condition.getStockCode(), k -> new TreeMap<>())
                        .computeIfAbsent(condition.getTargetPrice(), k -> new ArrayList<>())
                        .add(condition);
            } else {
                underMap.computeIfAbsent(condition.getStockCode(), k -> new TreeMap<>())
                        .computeIfAbsent(condition.getTargetPrice(), k -> new ArrayList<>())
                        .add(condition);
            }
        }
    }

    public void process(String stockCode, int currentPrice) {
        checkAndNotify(stockCode, currentPrice);
    }

    private void checkAndNotify(String stockCode, int currentPrice) {
        NavigableMap<Integer, List<AlertCondition>> overConditions = overMap.get(stockCode);
        if (overConditions != null) {
            SortedMap<Integer, List<AlertCondition>> matched = overConditions.headMap(currentPrice + 1);
            notifyAndRemove(matched, stockCode, currentPrice);
        }

        NavigableMap<Integer, List<AlertCondition>> underConditions = underMap.get(stockCode);
        if (underConditions != null) {
            SortedMap<Integer, List<AlertCondition>> matched = underConditions.tailMap(currentPrice);
            notifyAndRemove(matched, stockCode, currentPrice);
        }
    }

    private void notifyAndRemove(SortedMap<Integer, List<AlertCondition>> matched,
                                 String stockCode, int currentPrice) {
        for (Map.Entry<Integer, List<AlertCondition>> entry : matched.entrySet()) {
            for (AlertCondition cond : entry.getValue()) {
                System.out.printf("[ALERT] userId=%d, 종목=%s, 현재가=%d 목표가=%d 조건=%s%n",
                        cond.getUserId(), stockCode, currentPrice, cond.getTargetPrice(), cond.getCondition());

                User user = userRepository.findById(cond.getUserId())
                        .orElseThrow(() -> new EntityNotFoundException());

                Stock stock = stockRepository.findById(stockCode)
                        .orElseThrow(() -> new EntityNotFoundException());
                // 알람 보내는 부분
                AlarmRequestDTO alarmRequest = AlarmRequestDTO
                        .builder()
                        .content(
                                user.getUserId() + "님이 설정하신 "
                                        + stock.getStockName() + " 주식 가격이 목표하신"
                                        + cond.getTargetPrice() + " 에 도달하였습니다."
                        )
                        .type(AlarmType.STOCK_PRICE)
                        .build();

                alarmService.createAlarm(alarmRequest, user.getUserId());

            }
        }
        matched.clear();
    }
}
