package com.project.stock.investory.websocket;


import org.java_websocket.client.WebSocketClient;
import org.java_websocket.handshake.ServerHandshake;

import java.net.URI;
import java.util.Timer;
import java.util.TimerTask;

public class KisWebSocketClient extends WebSocketClient {

    public KisWebSocketClient(URI serverUri) {
        super(serverUri);
    }

    @Override
    public void onOpen(ServerHandshake handshake) {
        System.out.println("✅ WebSocket 연결 성공");

        String approvalMsg = """
        {
          "header": {
            "approval_key": "eaf82f2d-ed7b-422c-bad1-6582b77d636c",
            "custtype": "P",
            "tr_type": "1",
            "content-type": "utf-8"
          },
          "body": {
            "input": {}
          }
        }
        """;
        send(approvalMsg);
        System.out.println("📨 인증 메시지 전송");

        new Timer().schedule(new TimerTask() {
            @Override
            public void run() {
                String subscribeMsg = """
                {
                  "header": {
                    "tr_id": "H0STASP0",
                    "tr_key": "005930"
                  }
                }
                """;
                send(subscribeMsg);
                System.out.println("📨 구독 메시지 전송");
            }
        }, 2000);
    }

    @Override
    public void onMessage(String message) {
        System.out.println("📥 수신: " + message);
    }

    @Override
    public void onClose(int code, String reason, boolean remote){
        System.out.println("❌ 연결 종료: " + code + " / reason: " + reason + " / remote: " + remote);
    }

    @Override
    public void onError(Exception ex) {
        System.err.println("🚨 에러 발생");
        ex.printStackTrace();
    }
}
