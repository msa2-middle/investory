package com.project.stock.investory.stockAlertSetting.processor;

import com.project.stock.investory.alarm.helper.AlarmHelper;
import com.project.stock.investory.alarm.service.AlarmService;
import com.project.stock.investory.stockAlertSetting.event.StockAlertEvent;
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
import org.springframework.context.ApplicationEventPublisher;
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
    private final AlarmHelper alarmHelper;
    private final ApplicationEventPublisher eventPublisher; // 🔥 WebSocket 대신 이벤트 사용

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

    // 🔥 주기적으로 새로운 알람 설정을 로드 (30분마다 - 백업용 동기화) - 수정됨
    @Scheduled(fixedRate = 1800000)
    public void refreshConditions() {
        log.info("주기적 동기화 시작 (백업용)");
        loadAllConditions();
        refreshCaches();

        // 🔥 WebSocket 구독도 새로고침 (이벤트 발행)
        eventPublisher.publishEvent(StockAlertEvent.createRefresh());
    }

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

        for (StockAlertSetting setting : settings) {
            try {
                AlertCondition condition = new AlertCondition(
                        setting.getSettingId(),
                        setting.getUser().getUserId(),
                        setting.getStock().getStockId(),
                        setting.getTargetPrice(),
                        setting.getCondition());

                if (condition.getCondition() == ConditionType.ABOVE) {

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

                    // 알람 보내기 실행
                    alarmHelper.createStockPriceAlarm(stockCode, user, cond.getTargetPrice(), currentPrice, stock.getStockName(), conditionText);

                    // DB 업데이트 (영구적으로 비활성화)
                    StockAlertSetting stockAlertSetting =
                            stockAlertSettingRepository.findById(cond.getSettingId())
                                    .orElseThrow(() -> new EntityNotFoundException());

                    // is_active 0으로 처리 후 저장
                    stockAlertSetting.updateIsActive();

                    stockAlertSettingRepository.save(stockAlertSetting);

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

    // 🔥 새로운 알람 설정이 추가될 때 호출 (수정됨)
    public void addCondition(StockAlertSetting setting) {
        try {
            loadConditions(Collections.singletonList(setting));

            // 🔥 WebSocket 구독 추가 (이벤트 발행)
            eventPublisher.publishEvent(StockAlertEvent.createAdd(setting.getStock().getStockId()));

            log.info("새 알람 조건 추가됨: settingId={}, stockCode={}",
                    setting.getSettingId(), setting.getStock().getStockId());
        } catch (Exception e) {
            log.error("알람 조건 추가 실패: settingId={}", setting.getSettingId(), e);
        }
    }

    // 🔥 알람 설정이 삭제될 때 호출 (수정됨)
    public void removeCondition(Long settingId, String stockCode, ConditionType conditionType, Integer targetPrice) {
        try {
            Map<String, NavigableMap<Integer, List<AlertCondition>>> targetMap =
                    (conditionType == ConditionType.ABOVE) ? overMap : underMap;

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

            // 🔥 수정: 조건 제거 후 해당 종목에 알람이 없는지 확인
            if (!hasAnyAlertForStock(stockCode)) {
                log.info("종목 {}에 대한 모든 알람이 제거됨, WebSocket 구독 해제", stockCode);
                eventPublisher.publishEvent(StockAlertEvent.createRemove(stockCode, settingId, conditionType, targetPrice));
            }

            processedAlerts.remove(settingId);
            log.info("알람 조건 삭제됨: settingId={}, stockCode={}", settingId, stockCode);
        } catch (Exception e) {
            log.error("알람 조건 삭제 실패: settingId={}", settingId, e);
        }
    }


    // 🔥 특정 종목에 알람이 있는지 확인
    private boolean hasAnyAlertForStock(String stockCode) {
        return (overMap.containsKey(stockCode) && !overMap.get(stockCode).isEmpty()) ||
                (underMap.containsKey(stockCode) && !underMap.get(stockCode).isEmpty());
    }

    // 사용자 캐시 업데이트 (생성/수정 시 사용)
    public void updateUserCache(User user) {
        userCache.put(user.getUserId(), user);
        log.info("사용자 캐시 업데이트: userId={}", user.getUserId());
    }

    // 사용자 캐시 삭제
    public void removeUserCache(Long userId) {
        userCache.remove(userId);
        log.info("사용자 캐시 삭제: userId={}", userId);
    }

    // 🔥 주식 알람 설정 변경 시 조건 맵 업데이트 (수정됨)
    public void updateStockAlertCondition(StockAlertSetting setting) {
        try {
            String stockCode = setting.getStock().getStockId();

            // 먼저 기존 조건 제거 (settingId로 찾아서 제거)
            removeConditionBySettingId(setting.getSettingId());

            // 활성화된 설정이면 새로 추가
            if (setting.getIsActive() == 1) {
                addCondition(setting);
                log.info("알람 조건 업데이트 완료: settingId={}, 활성화됨", setting.getSettingId());
            } else {
                // 🔥 비활성화된 경우 해당 종목에 다른 알람이 없으면 구독 해제 (이벤트 발행)
                if (!hasAnyAlertForStock(stockCode)) {
                    eventPublisher.publishEvent(StockAlertEvent.createUpdate(stockCode));
                }
                log.info("알람 조건 업데이트 완료: settingId={}, 비활성화됨", setting.getSettingId());
            }
        } catch (Exception e) {
            log.error("알람 조건 업데이트 실패: settingId={}", setting.getSettingId(), e);
        }
    }

    // settingId로 조건 제거 (내부 헬퍼 메서드)
    private void removeConditionBySettingId(Long settingId) {
        try {
            // overMap에서 제거
            removeFromMap(overMap, settingId);
            // underMap에서 제거
            removeFromMap(underMap, settingId);
            // processedAlerts에서도 제거
            processedAlerts.remove(settingId);
        } catch (Exception e) {
            log.error("settingId로 조건 제거 실패: settingId={}", settingId, e);
        }
    }

    // Map에서 settingId와 일치하는 조건 제거
    private void removeFromMap(Map<String, NavigableMap<Integer, List<AlertCondition>>> targetMap, Long settingId) {
        for (Iterator<Map.Entry<String, NavigableMap<Integer, List<AlertCondition>>>> stockIter = targetMap.entrySet().iterator(); stockIter.hasNext(); ) {
            Map.Entry<String, NavigableMap<Integer, List<AlertCondition>>> stockEntry = stockIter.next();
            NavigableMap<Integer, List<AlertCondition>> priceMap = stockEntry.getValue();

            for (Iterator<Map.Entry<Integer, List<AlertCondition>>> priceIter = priceMap.entrySet().iterator(); priceIter.hasNext(); ) {
                Map.Entry<Integer, List<AlertCondition>> priceEntry = priceIter.next();
                List<AlertCondition> conditions = priceEntry.getValue();

                // settingId와 일치하는 조건 제거
                conditions.removeIf(cond -> cond.getSettingId().equals(settingId));

                // 조건 리스트가 비어있으면 가격 엔트리 제거
                if (conditions.isEmpty()) {
                    priceIter.remove();
                }
            }

            // 가격 맵이 비어있으면 주식 엔트리 제거
            if (priceMap.isEmpty()) {
                stockIter.remove();
            }
        }
    }
}