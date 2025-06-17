//package com.project.stock.investory.stockInfo.service;
//
//import org.java_websocket.client.WebSocketClient;
//import org.java_websocket.handshake.ServerHandshake;
//
//import java.net.URI;
//import java.net.URISyntaxException;
//import java.util.Timer;
//import java.util.TimerTask;
//
//public class WebSocketClinet extends WebSocketClient {
//
//    WebSocketClinet(URI serverUri) {
//        super(serverUri);
//    }
//
//
//    @Override
//    public void onOpen(ServerHandshake serverHandshake) {
//        System.out.println("✅ WebSocket 연결 성공");
//
//        // Step 1. 인증 메시지 정송
//        String approvalMsg = """
//                 {
//                            "header": {
//                              "approval_key": "eaf82f2d-ed7b-422c-bad1-6582b77d636c",
//                              "custtype": "P",
//                              "tr_type": "1",
//                              "content-type": "utf-8"
//                            },
//                            "body": {
//                              "input": {}
//                            }
//                          }
//                """;
//        send(approvalMsg);
//        System.out.println("\uD83D\uDCE8 호가 구독 메시지 전송");
//
//
//        // Step 2. 일정 지연 후 구독 메시지 전송
//        new Timer().schedule(new TimerTask() {
//
//            @Override
//            public void run() {
//                String subscribeMessage = """
//                        {
//                          "header": {
//                            "tr_id": "H0STASP0",
//                            "tr_key": "005930"  //변경
//                          }
//                        }
//                        """;
//                send(subscribeMessage);
//                System.out.println("📨 호가 구독 메시지 전송");
//            }
//
//        }, 2000); // 2초 후 전송
//
//    }
//
//    @Override
//    public void onMessage(String msg) {
//        System.out.println("📥 실시간 수신: " + msg);
//    }
//
//    @Override
//    public void onClose(int code, String reason, boolean remote) {
//        System.out.println("❌ 연결 종료: " + reason);
//    }
//
//    @Override
//    public void onError(Exception e) {
//        System.err.println("🚨 오류 발생: ");
//        e.printStackTrace();
//    }
//
//    public static void main(String[] args) {
//        try {
//            String wsUrl = "wss://ops.koreainvestment.com:9443/WebSocket";
//            WebSocketClient client = new WebSocketClinet(new URI(wsUrl));
//            client.connect();
//
//            int retry = 0;
//            while (!client.isOpen()) {
//                System.out.println("Connecting... retry = " + retry++);
//                Thread.sleep(1000);
//                if (retry > 10) {
//                    System.out.println("⛔ 연결 실패");
//                    break;
//                }
//            }
//        } catch (Exception e) {
//            System.err.println("❌ 예외 발생:");
//            e.printStackTrace();  // 여기서 콘솔에 예외 출력됨
//        }
//    }
//}
//
//
