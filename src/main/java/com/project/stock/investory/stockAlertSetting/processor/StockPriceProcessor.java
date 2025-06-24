package com.project.stock.investory.stockAlertSetting.processor;

// todo: 현재 코드로는 사용자의 생성, 수정, 삭제 시 userCache에 적용이 안됨 => 이 부분은 각각에 채워줘야 함.
// todo: 역시나 마찬가지로 목표 가격 설정도 똑같이 적용

import com.project.stock.investory.alarm.dto.AlarmRequestDTO;
import com.project.stock.investory.alarm.entity.AlarmType;
import com.project.stock.investory.alarm.service.AlarmService;
import com.project.stock.investory.stockAlertSetting.model.AlertCondition;
import com.project.stock.investory.stockAlertSetting.model.ConditionType;
import com.project.stock.investory.stockAlertSetting.model.StockAlertSetting;
import com.project.stock.investory.stockAlertSetting.repository.StockAlertSettingRepository;
import com.project.stock.investory.stockInfo.model.Stock;
import com.project.stock.investory.stockInfo.repository.StockRepository;
import com.project.stock.investory.user.entity.User;
import com.project.stock.investory.user.repository.UserRepository;
import jakarta.annotation.PostConstruct;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

@Slf4j
@Component
@RequiredArgsConstructor
public class StockPriceProcessor {

    private final AlarmService alarmService;
    private final UserRepository userRepository;
    private final StockRepository stockRepository;
    private final StockAlertSettingRepository stockAlertSettingRepository;

    // 가격 이상 조건들 (목표가를 오름차순으로 정렬)
    private final Map<String, NavigableMap<Integer, List<AlertCondition>>> overMap = new ConcurrentHashMap<>();
    // 가격 이하 조건들 (목표가를 내림차순으로 정렬)
    private final Map<String, NavigableMap<Integer, List<AlertCondition>>> underMap = new ConcurrentHashMap<>();

    // 이미 알림을 보낸 조건들을 추적 (중복 방지)
    private final Set<Long> processedAlerts = ConcurrentHashMap.newKeySet();

    // 사용자 및 주식 정보 캐시 (성능 최적화)
    private final Map<Long, User> userCache = new ConcurrentHashMap<>();
    private final Map<String, Stock> stockCache = new ConcurrentHashMap<>();

    @PostConstruct
    public void init() {
        loadAllConditions();
        loadCaches();
        log.info("StockPriceProcessor 초기화 완료");
    }

//    // 주기적으로 새로운 알람 설정을 로드 (5분마다)
//    @Scheduled(fixedRate = 600000)
//    public void refreshConditions() {
//        log.info("알람 조건 새로고침 시작");
//        loadAllConditions();
//        refreshCaches();
//    }

    private void loadAllConditions() {
        try {
            // 활성화된 알람 설정만 조회
            List<StockAlertSetting> activeSettings = stockAlertSettingRepository.findByIsActiveTrue();

            // 기존 맵 초기화
            overMap.clear();
            underMap.clear();
            processedAlerts.clear();

            log.info("총 {}개의 활성 알람 조건 로드됨", activeSettings.size());
            loadConditions(activeSettings);

        } catch (Exception e) {
            log.error("알람 조건 로드 중 오류 발생", e);
        }
    }

    public void loadConditions(List<StockAlertSetting> settings) {

        log.info("총 {}개의 활성 알람 조건 로드됨 여기는 내가 만든 로그", settings.size());

        for (StockAlertSetting setting : settings) {
            try {
                AlertCondition condition = new AlertCondition(
                        setting.getSettingId(),
                        setting.getUser().getUserId(),
                        setting.getStock().getStockId(),
                        setting.getTargetPrice(),
                        setting.getCondition());

                if (condition.getCondition() == ConditionType.ABOVE) {

                    log.info("여기로 오나요??");

                    overMap.computeIfAbsent(
                            condition.getStockCode(),
                            k -> new TreeMap<>() // 오름차순 정렬
                    ).computeIfAbsent(
                            condition.getTargetPrice(),
                            k -> new ArrayList<>()
                    ).add(condition);
                } else {
                    underMap.computeIfAbsent(
                            condition.getStockCode(),
                            k -> new TreeMap<>(Collections.reverseOrder()) // 내림차순 정렬
                    ).computeIfAbsent(
                            condition.getTargetPrice(),
                            k -> new ArrayList<>()
                    ).add(condition);
                }
            } catch (Exception e) {
                log.error("알람 조건 로드 실패: settingId={}", setting.getSettingId(), e);
            }
        }
    }

    private void loadCaches() {
        try {
            // 사용자 캐시 로드
            userRepository.findAll().forEach(user -> userCache.put(user.getUserId(), user));

            // 주식 정보 캐시 로드
            stockRepository.findAll().forEach(stock -> stockCache.put(stock.getStockId(), stock));

            log.info("캐시 로드 완료: 사용자 {}명, 주식 {}개", userCache.size(), stockCache.size());
        } catch (Exception e) {
            log.error("캐시 로드 중 오류 발생", e);
        }
    }

    private void refreshCaches() {
        // 간단한 캐시 갱신 (실제로는 변경된 데이터만 갱신하는 것이 더 효율적)
        userCache.clear();
        stockCache.clear();
        loadCaches();
    }

    public void process(String stockCode, int currentPrice) {
        try {
            log.debug("주식 가격 처리: 종목={}, 현재가={}", stockCode, currentPrice);
            checkAndNotify(stockCode, currentPrice);
        } catch (Exception e) {
            log.error("주식 가격 처리 중 오류 발생: 종목={}, 현재가={}", stockCode, currentPrice, e);
        }
    }

    private void checkAndNotify(String stockCode, int currentPrice) {
        // ABOVE 조건 체크 (현재가가 목표가 이상인 경우)
        NavigableMap<Integer, List<AlertCondition>> overConditions = overMap.get(stockCode);
        if (overConditions != null) {
            // 현재가 이하의 모든 목표가들을 가져옴 (즉, 조건을 만족하는 것들)
            SortedMap<Integer, List<AlertCondition>> matched = overConditions.headMap(currentPrice, true);

            log.debug("=====================주식  현재가={}", currentPrice);

            notifyAndRemove(matched, stockCode, currentPrice, "이상");
        }

        // UNDER 조건 체크 (현재가가 목표가 이하인 경우)
        NavigableMap<Integer, List<AlertCondition>> underConditions = underMap.get(stockCode);
        if (underConditions != null) {
            // 현재가 이상의 모든 목표가들을 가져옴 (즉, 조건을 만족하는 것들)
            SortedMap<Integer, List<AlertCondition>> matched = underConditions.headMap(currentPrice, true);
            notifyAndRemove(matched, stockCode, currentPrice, "이하");
        }
    }

    private void notifyAndRemove(
                    SortedMap<Integer,
                    List<AlertCondition>> matched,
                    String stockCode,
                    int currentPrice,
                    String conditionText
    ) {
        List<AlertCondition> toRemove = new ArrayList<>();

        for (Map.Entry<Integer, List<AlertCondition>> entry : matched.entrySet()) {
            for (AlertCondition cond : entry.getValue()) {
                try {
                    // 중복 알림 방지
                    if (processedAlerts.contains(cond.getSettingId())) {
                        continue;
                    }

                    log.info("[ALERT] userId={}, 종목={}, 현재가={}, 목표가={}, 조건={}",
                            cond.getUserId(), stockCode, currentPrice, cond.getTargetPrice(), cond.getCondition());

                    // 캐시에서 사용자 정보 조회
                    User user = userCache.get(cond.getUserId());
                    if (user == null) {
                        user = userRepository.findById(cond.getUserId()).orElseThrow(() -> new EntityNotFoundException("사용자를 찾을 수 없습니다: " + cond.getUserId()));
                        userCache.put(cond.getUserId(), user); // 캐시 업데이트
                    }

                    // 캐시에서 주식 정보 조회
                    Stock stock = stockCache.get(stockCode);
                    if (stock == null) {
                        stock = stockRepository.findById(stockCode).orElseThrow(() -> new EntityNotFoundException("주식을 찾을 수 없습니다: " + stockCode));
                        stockCache.put(stockCode, stock); // 캐시 업데이트
                    }

                    // 알람 생성
                    AlarmRequestDTO alarmRequest = AlarmRequestDTO
                            .builder()
                            .content(String.format
                                    (
                                    "[주식 알림] %s님, %s 주식이 목표가 %,d원 %s에 도달했습니다. (현재가: %,d원)",
                                    user.getName() != null ? user.getName() : "사용자",
                                    stock.getStockName(),
                                    cond.getTargetPrice(),
                                    conditionText,
                                    currentPrice
                                    )
                            )
                            .type(AlarmType.STOCK_PRICE)
                            .build();

                    // 알람 보내기 실행
                    alarmService.createAlarm(alarmRequest, user.getUserId());

//                    // DB 업데이트 (영구적으로 비활성화)
//                    StockAlertSetting stockAlertSetting =
//                            stockAlertSettingRepository.findById(cond.getSettingId())
//                                    .orElseThrow(() -> new EntityNotFoundException());
//
//                    stockAlertSetting =
//                            StockAlertSetting
//                                    .builder()
//                                    .isActive(0)
//                                    .build();
//
//                    stockAlertSettingRepository.save(stockAlertSetting);

                    // 처리 완료 표시 (중복 방지)
                    processedAlerts.add(cond.getSettingId());
                    toRemove.add(cond);

                    log.info("알람 생성 완료: userId={}, 종목={}", cond.getUserId(), stockCode);

                } catch (Exception e) {
                    log.error("개별 알람 처리 실패: settingId={}, userId={}, 종목={}", cond.getSettingId(), cond.getUserId(), stockCode, e);
                    // 개별 실패가 전체를 막지 않도록 계속 진행
                }
            }
        }

        // 처리된 조건들 제거
        for (AlertCondition cond : toRemove) {
            List<AlertCondition> conditions = matched.get(cond.getTargetPrice());
            if (conditions != null) {
                conditions.remove(cond);
                if (conditions.isEmpty()) {
                    matched.remove(cond.getTargetPrice());
                }
            }
        }
    }

    // 새로운 알람 설정이 추가될 때 호출
    public void addCondition(StockAlertSetting setting) {
        try {
            loadConditions(Collections.singletonList(setting));
            log.info("새 알람 조건 추가됨: settingId={}", setting.getSettingId());
        } catch (Exception e) {
            log.error("알람 조건 추가 실패: settingId={}", setting.getSettingId(), e);
        }
    }

    // 알람 설정이 삭제될 때 호출
    public void removeCondition(Long settingId, String stockCode, ConditionType conditionType, Integer targetPrice) {
        try {
            Map<String, NavigableMap<Integer, List<AlertCondition>>> targetMap = (conditionType == ConditionType.ABOVE) ? overMap : underMap;

            NavigableMap<Integer, List<AlertCondition>> stockConditions = targetMap.get(stockCode);
            if (stockConditions != null) {
                List<AlertCondition> conditions = stockConditions.get(targetPrice);
                if (conditions != null) {
                    conditions.removeIf(cond -> cond.getSettingId().equals(settingId));
                    if (conditions.isEmpty()) {
                        stockConditions.remove(targetPrice);
                    }
                    if (stockConditions.isEmpty()) {
                        targetMap.remove(stockCode);
                    }
                }
            }

            processedAlerts.remove(settingId);
            log.info("알람 조건 삭제됨: settingId={}", settingId);
        } catch (Exception e) {
            log.error("알람 조건 삭제 실패: settingId={}", settingId, e);
        }
    }
}
