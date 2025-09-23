package com.project.stock.investory.stockInfo.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.project.stock.investory.stockInfo.dto.RealTimeTradeDTO;
import com.project.stock.investory.stockInfo.exception.StockNotFoundException;
import com.project.stock.investory.stockInfo.repository.StockRepository;
import com.project.stock.investory.stockInfo.util.StockMarketUtils;
import com.project.stock.investory.stockInfo.websocket.KisWebSocketClient;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.ReentrantLock;

/**
 * 주식 실시간 체결 데이터를 KIS WebSocket 으로부터 받아
 * 구독 중인 클라이언트(SSE) 들에게 전달(fan‑out)하는 서비스.
 *
 * 🔍 이 서비스가 하는 일:
 * 1. 한국투자증권(KIS) WebSocket에서 실시간 주식 데이터를 받음
 * 2. 여러 사용자가 같은 주식을 구독할 수 있게 함 (1:N 관계)
 * 3. 받은 데이터를 모든 구독자에게 동시에 전송 (팬아웃)
 * 4. 사용자 연결이 끊어지면 자동으로 정리
 *
 * 주요 책임:
 *   1. KIS 구독/해지 관리 - 실제 데이터 소스와의 연결 관리
 *   2. SSE 연결(Emitter) 생성·보존·해제 - 사용자와의 연결 관리
 *   3. 실시간 체결 데이터 fan‑out(전달) - 데이터 배포
 *   4. 다중 스레드 환경에서 안전한 동시성 제어 - 여러 사용자 동시 처리
 */
@Slf4j  // 로그 출력을 위한 Lombok 어노테이션
@Service  // Spring에서 비즈니스 로직을 처리하는 서비스 클래스임을 표시
public class StockWebSocketService {

    // 📡 KIS(한국투자증권) WebSocket 클라이언트 - 실제 주식 데이터를 받아오는 역할
    private final KisWebSocketClient kisClient;

    // 🗃️ 주식 정보를 데이터베이스에서 조회하는 Repository
    private final StockRepository stockRepository;

    // 🔄 JSON 변환을 위한 ObjectMapper (Java 객체 ↔ JSON 문자열)
    private final ObjectMapper om = new ObjectMapper();

    // 📊 핵심 데이터 구조들 (멀티스레드 환경에서 안전하게 동작)

    /**
     * 🏠 각 주식별로 구독 중인 사용자들의 SSE 연결을 저장하는 맵
     * Key: 주식코드(예: "005930" - 삼성전자)
     * Value: 해당 주식을 구독하는 사용자들의 SSE 연결 리스트
     *
     * ConcurrentHashMap: 여러 스레드가 동시에 읽고 쓸 수 있는 안전한 맵
     * CopyOnWriteArrayList: 읽기는 빠르고, 쓰기 시에만 복사하는 안전한 리스트
     */
    private final Map<String, CopyOnWriteArrayList<SseEmitter>> emitters = new ConcurrentHashMap<>();

    /**
     * 📋 현재 KIS에 구독 요청을 보낸 주식 코드들의 집합
     * 같은 주식을 여러 명이 구독해도 KIS에는 한 번만 구독 요청을 보내기 위함
     */
    private final Set<String> subscribed = ConcurrentHashMap.newKeySet();

    /**
     * 🔐 각 주식 코드별로 동시성 제어를 위한 락(Lock) 저장소
     * 같은 주식에 대한 구독/해지 작업이 동시에 일어날 때 충돌을 방지
     */
    private final Map<String, ReentrantLock> locks = new ConcurrentHashMap<>();

    /**
     * 🏗️ 생성자 - 필요한 의존성들을 주입받아 초기화
     */
    public StockWebSocketService(KisWebSocketClient kisClient, StockRepository stockRepository) {
        this.kisClient = kisClient;
        this.stockRepository = stockRepository;
    }

    /**
     * 📈 특정 주식의 실시간 가격 스트림을 구독하는 메인 메서드
     *
     * @param stockId 구독하고 싶은 주식 코드 (예: "005930")
     * @return SseEmitter 실시간 데이터를 받을 수 있는 SSE 연결 객체
     */
    public SseEmitter getStockPriceStream(String stockId) {

        // ⏰ ① 장 운영 시간 체크 - 주식 거래소가 열려있는지 확인
        if (!StockMarketUtils.isTradingHours()) {
            // 장이 닫혀있으면 즉시 종료되는 연결을 반환하고 메시지 전송
            return closedEmitter("marketClosed", "장 외 시간입니다.");
        }

        // 🔍 ② 유효한 주식 코드인지 데이터베이스에서 확인
        if(!stockRepository.existsByStockId((stockId))){
            // 존재하지 않는 주식 코드면 예외 발생
            throw new StockNotFoundException(stockId);
        }

        // 📡 ③ 새로운 SSE 연결 생성 (30분 후 자동 타임아웃)
        // SSE(Server-Sent Events): 서버에서 클라이언트로 실시간 데이터를 보내는 기술
        SseEmitter emitter = new SseEmitter(TimeUnit.MINUTES.toMillis(30));

        // 📝 해당 주식의 구독자 리스트에 새 연결 추가
        // computeIfAbsent: 키가 없으면 새 리스트를 생성하고, 있으면 기존 리스트 반환
        emitters.computeIfAbsent(stockId, k -> new CopyOnWriteArrayList<>()).add(emitter);
        log.info("[{}] SSE 연결 +1 (총 {}개)", stockId, emitters.get(stockId).size());

        // 🎯 ④ KIS 구독 관리 - 이 주식을 처음으로 구독하는지 확인
        if (subscribed.add(stockId)) {  // Set.add()는 새로 추가되면 true, 이미 있으면 false 반환
            log.info("[{}] ▶️  KIS subscribe", stockId);
            // 첫 구독이므로 KIS에 실제 구독 요청 전송
            // Lambda 표현식: 데이터를 받으면 fanOut 메서드로 모든 구독자에게 전달
            kisClient.queueSubscribe(stockId, dto -> fanOut(stockId, dto));
        } else {
            log.debug("[{}] 이미 subscribe 중", stockId);
        }

        // 🔌 ⑤ 연결 종료 상황에 대한 콜백 함수들 등록

        // 타임아웃 발생 시 (30분 후)
        emitter.onTimeout(() -> handleDisconnect(stockId, emitter));

        // 클라이언트가 정상적으로 연결을 종료한 경우
        emitter.onCompletion(() -> handleDisconnect(stockId, emitter));

        // 에러가 발생한 경우 (네트워크 문제 등)
        emitter.onError(e -> removeEmitterOnly(stockId, emitter));

        // 📨 연결 성공 메시지 전송
        sendEvent(emitter, "message", "connected");

        return emitter;  // 설정이 완료된 SSE 연결 반환
    }

    /**
     * 📢 팬아웃(Fan-out): 받은 데이터를 해당 주식을 구독하는 모든 클라이언트에게 전달
     *
     * @param stockId 주식 코드
     * @param dto 실시간 거래 데이터 (가격, 거래량 등)
     */
    public void fanOut(String stockId, RealTimeTradeDTO dto) {
        // 해당 주식을 구독하는 모든 SSE 연결들 가져오기
        List<SseEmitter> list = emitters.getOrDefault(stockId, new CopyOnWriteArrayList<>());

        // 🔄 모든 구독자에게 데이터 전송
        for (SseEmitter emitter : list) {
            try {
                // 각 연결에 거래 데이터 전송
                sendEvent(emitter, "trade", dto);
            } catch (Exception e) {
                // 전송 실패 시 (연결이 끊어진 경우 등) 해당 연결 제거
                log.debug("[{}] emitter 전송 실패, 제거", stockId);
                handleDisconnect(stockId, emitter);
            }
        }
    }

    /**
     * 📤 SSE 이벤트 전송 유틸리티 메서드
     *
     * @param emitter 전송할 SSE 연결
     * @param name 이벤트 이름 (클라이언트에서 구분용)
     * @param payload 전송할 데이터
     */
    private void sendEvent(SseEmitter emitter, String name, Object payload) {
        try {
            Object body = payload;  // 전송할 실제 데이터
            MediaType mt = MediaType.TEXT_PLAIN;  // 기본 콘텐츠 타입

            // 📋 복잡한 객체는 JSON으로 변환
            if (payload instanceof Map || payload instanceof RealTimeTradeDTO) {
                body = om.writeValueAsString(payload);  // 객체를 JSON 문자열로 변환
                mt = MediaType.APPLICATION_JSON;  // 콘텐츠 타입을 JSON으로 설정
            }

            // 🚀 실제 SSE 이벤트 전송
            emitter.send(SseEmitter.event().name(name).data(body, mt));
        } catch (Exception e) {
            log.debug("sendEvent 실패 : {}", e.toString());
        }
    }

    /**
     * ⛔ 즉시 종료되는 Emitter 생성 유틸리티
     * 장이 닫혀있거나 오류 상황에서 사용
     *
     * @param event 이벤트 이름
     * @param msg 전송할 메시지
     * @return 즉시 종료되는 SseEmitter
     */
    private SseEmitter closedEmitter(String event, String msg) {
        SseEmitter emitter = new SseEmitter(0L);  // 타임아웃 0 = 즉시 종료
        sendEvent(emitter, event, msg);  // 메시지 전송
        safeComplete(emitter);  // 안전하게 연결 종료
        return emitter;
    }

    /**
     * ❌ 에러 상황에서만 사용: emitter.complete() 호출 없이 목록에서만 제거
     * complete()를 호출하면 AsyncRequestNotUsableException이 발생할 수 있음
     *
     * @param stockId 주식 코드
     * @param emitter 제거할 SSE 연결
     */
    private void removeEmitterOnly(String stockId, SseEmitter emitter) {
        // 🔐 동시성 제어: 같은 주식에 대한 작업이 겹치지 않도록 락 획득
        ReentrantLock lock = locks.computeIfAbsent(stockId, k -> new ReentrantLock());
        lock.lock();
        try {
            // 📝 해당 주식의 구독자 리스트에서 연결 제거
            List<SseEmitter> list = emitters.get(stockId);
            if (list != null) {
                list.remove(emitter);

                // 🧹 마지막 구독자가 떠나면 자원 정리
                if (list.isEmpty()) {
                    emitters.remove(stockId);  // 빈 리스트 제거

                    // KIS 구독도 해지 (더 이상 데이터가 필요 없음)
                    if (subscribed.remove(stockId)) {
                        log.info("[{}] ⏹️  마지막 구독자 종료 → KIS unsubscribe", stockId);
                        kisClient.queueUnsubscribe(stockId);
                    }
                }
            }
        } finally {
            lock.unlock();  // 반드시 락 해제
        }
        // ⚠️ 여기서는 emitter.complete() 호출하지 않음 → 예외 방지
    }

    /**
     * 🔌 정상적인 연결 종료 처리: timeout 또는 completion 이벤트에서 호출
     *
     * @param stockId 주식 코드
     * @param emitter 종료할 SSE 연결
     */
    private void handleDisconnect(String stockId, SseEmitter emitter) {
        removeEmitterOnly(stockId, emitter);  // 목록에서 제거 (위 메서드 재사용)
        safeComplete(emitter);               // 연결 완전 종료
    }

    /**
     * 🛡️ 안전한 SSE 연결 종료 처리
     * 이미 종료된 연결에서 발생할 수 있는 예외들을 안전하게 처리
     *
     * @param emitter 종료할 SSE 연결
     */
    private void safeComplete(SseEmitter emitter) {
        try {
            emitter.complete();  // SSE 연결 정상 종료
        } catch (IllegalStateException ignored) {
            // 이미 종료된 연결인 경우 - 무시해도 됨
            log.debug("Emitter unusable: {}", ignored.getMessage());
        } catch (Exception ex) {
            // 기타 예상치 못한 오류
            log.warn("SSE complete error", ex);
        }
    }
}