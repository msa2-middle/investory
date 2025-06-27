package com.project.stock.investory.stockInfo.websocket;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.project.stock.investory.stockInfo.dto.RealTimeTradeDTO;
import jakarta.websocket.*;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.net.URI;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ScheduledExecutorService;
import java.util.function.Consumer;

@ClientEndpoint
@Component
@Slf4j
public class KisWebSocketClient {

    @Value("${kis.ws.url:ws://ops.koreainvestment.com:21000/WebSocket}")
    private String wsUrl;

    @Value("${kis.approval-key}")
    private String approvalKey;

    private final ObjectMapper om = new ObjectMapper();
    private Consumer<RealTimeTradeDTO> handler;            // 서비스층 콜백
    private final Set<String> subs = ConcurrentHashMap.newKeySet();
    private Session session;

    /* ========= 외부 API ========= */

    public synchronized void subscribe(String stockId,
                                       Consumer<RealTimeTradeDTO> cb) throws Exception {
        this.handler = cb;          // 서비스에서 넘겨준 콜백 저장

        // 이미 WebSocket 세션이 열려 있으면 새 연결 대신 구독 메시지만 전송
        if (session != null && session.isOpen()) {
            sendSubMsg(stockId);
            return;
        }

        // 최초 연결
        connect();
        sendSubMsg(stockId);
    }

    /* ========= 내부 ========= */

    private void connect() throws Exception {
        WebSocketContainer c = ContainerProvider.getWebSocketContainer();
        c.connectToServer(this, URI.create(wsUrl));
    }


    public synchronized void disconnect() {
        try {
            if (session != null && session.isOpen()) {
                for (String id : subs) {              // 🔹 subs 로 변경
                    String unsub = """
                    {
                      "header": {
                        "approval_key":"%s",
                        "custtype":"P",
                        "tr_type":"2",           // 해제
                        "content-type":"utf-8",
                        "tr_id":"H0STCNT0",
                        "tr_key":"%s"
                      }
                    }
                    """.formatted(approvalKey, id);
                    session.getAsyncRemote().sendText(unsub);
                }
                // Close Frame
                session.close(new CloseReason(
                        CloseReason.CloseCodes.NORMAL_CLOSURE, "manual close"));
                log.info("KIS WS 정상 종료");
            }
        } catch (Exception e) {
            log.warn("WS 정상 종료 실패", e);
        } finally {
            subs.clear();
            session = null;
        }
    }

    private void sendSubMsg(String stockId) {
        if (subs.contains(stockId)) return;

        String payload = """
        {
          "header": {
            "approval_key": "%s",
            "custtype": "P",
            "tr_type": "1",
            "content-type":"utf-8",
            "tr_id": "H0STCNT0",
            "tr_key": "%s"
          },
          "body": {
            "input": {
              "tr_id": "H0STCNT0",
              "tr_key": "%s"
            }
          }
        }""".formatted(approvalKey, stockId, stockId);

        session.getAsyncRemote().sendText(payload);
        subs.add(stockId);
    }

    /* ========= WS 콜백 ========= */

    @OnOpen
    public void onOpen(Session s) { this.session = s; log.info("KIS WS 연결"); }

    @OnMessage
    public void onMsg(String raw) {

        /* 0) 하트비트 무시 */
        if (raw.contains("\"tr_id\":\"PINGPONG\"")) return;

        /* 1) 문자열(H0STCNT0) 패킷 처리 ---------------------------------- */
        if (raw.startsWith("0|H0STCNT0|")) {
            log.debug("[KIS RAW] {}", raw);

            // 파이프 3개(0|H0STCNT0|001|) 이후 부분만 캐럿(^)으로 분리
            String[] pipe = raw.split("\\|", 4);
            if (pipe.length < 4) return;
            String[] f = pipe[3].split("\\^");

            // 안전 체크 (최소 40여 개 필드)
            if (f.length < 40) {
                log.warn("필드 수 부족: {}", f.length);
                return;
            }

            /* === 원하는 값 추출 === */
            RealTimeTradeDTO dto = new RealTimeTradeDTO(
                    f[0],          // stockId  (STCK_SHRN_ISCD)
                    f[2],          // tradePrice (STCK_PRPR)
                    f[12],         // tradeVolume (CNTG_VOL)  ← 6 ▶ 12 로
                    f[5] + "%",    // changeRate  (PRDY_CTRT) ← 4 ▶ 5 로
                    f[13],         // accumulateVolume (ACML_VOL)
                    f[1]           // tradeTime (STCK_CNTG_HOUR)
            );

            log.info("[KIS DTO] {}", dto);
            if (handler != null) handler.accept(dto);
            return;
        }

        /* 2) JSON 응답 패킷 처리 ---------------------------------------- */
        if (raw.startsWith("{")) {
            log.debug("[KIS RAW] {}", raw);
            try {
                JsonNode b = om.readTree(raw).path("body");
                if (!"0".equals(b.path("rt_cd").asText())) {  // 오류 응답
                    log.error("KIS 오류 {} - {}", b.path("msg_cd").asText(),
                            b.path("msg1").asText());
                    return;
                }
                RealTimeTradeDTO dto = new RealTimeTradeDTO(
                        b.path("STCK_SHRN_ISCD").asText(),
                        b.path("STCK_PRPR").asText(),
                        b.path("CNTG_VOL").asText(),
                        b.path("PRDY_CTRT").asText() + "%",
                        b.path("ACML_VOL").asText(),
                        b.path("STCK_CNTG_HOUR").asText()
                );
                log.info("[KIS DTO] {}", dto);
                if (handler != null) handler.accept(dto);
            } catch (Exception e) {
                log.warn("JSON 파싱 오류", e);
            }
        }
    }

    @OnError
    public void onErr(Session s, Throwable t) { log.error("WS 오류", t); }
}
