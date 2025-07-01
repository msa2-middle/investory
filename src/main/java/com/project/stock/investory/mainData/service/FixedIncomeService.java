package com.project.stock.investory.mainData.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.project.stock.investory.mainData.dto.FixedIncomeDto;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.util.ArrayList;
import java.util.List;

@Service
public class FixedIncomeService {

    @Value("${appkey}")
    private String appKey;

    @Value("${appsecret}")
    private String appSecret;

    @Value("${access_token}")
    private String accessToken;

    private final WebClient webClient;
    private final ObjectMapper objectMapper;

    @Autowired
    public FixedIncomeService(WebClient.Builder webClientBuilder, ObjectMapper objectMapper) {
        this.webClient = webClientBuilder.baseUrl("https://openapi.koreainvestment.com:9443").build();
        this.objectMapper = objectMapper;
    }

    // HTTP 헤더 생성
    private HttpHeaders createHttpHeaders() {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.setBearerAuth(accessToken);
        headers.set("appkey", appKey);
        headers.set("appSecret", appSecret);
        headers.set("tr_id", "FHPST07020000");
        headers.set("custtype", "P");
        return headers;
    }

    // outputType에 따라 output1 또는 output2 파싱
    private Mono<List<FixedIncomeDto>> parseOutputData(String response, int outputType) {
        try {
            List<FixedIncomeDto> resultList = new ArrayList<>();
            JsonNode rootNode = objectMapper.readTree(response);

            // outputType에 따라 노드 선택
            String nodeName = (outputType == 1) ? "output1" : "output2";
            JsonNode outputNode = rootNode.get(nodeName);

            System.out.println("Parsing " + nodeName + ": " + outputNode);

            if (outputNode != null && !outputNode.isNull()) {
                if (outputNode.isArray()) {
                    for (JsonNode node : outputNode) {
                        FixedIncomeDto dto = objectMapper.treeToValue(node, FixedIncomeDto.class);
                        resultList.add(dto);
                    }
                } else {
                    FixedIncomeDto dto = objectMapper.treeToValue(outputNode, FixedIncomeDto.class);
                    resultList.add(dto);
                }
            } else {
                System.out.println(nodeName + " not found in response");
            }

            return Mono.just(resultList);

        } catch (Exception e) {
            return Mono.error(new RuntimeException("output" + outputType + " 데이터 파싱 실패", e));
        }
    }

    // outputType(1=output1, 2=output2)에 따라 데이터 조회
    public List<FixedIncomeDto> getOutputData(int outputType) {
        HttpHeaders headers = createHttpHeaders();

        return webClient.get()
                .uri(uriBuilder -> uriBuilder
                        .path("/uapi/domestic-stock/v1/quotations/comp-interest")
                        .queryParam("FID_COND_MRKT_DIV_CODE", "I")
                        .queryParam("FID_COND_SCR_DIV_CODE", "20702")
                        .queryParam("FID_DIV_CLS_CODE", "1")
                        .queryParam("FID_DIV_CLS_CODE1", "")
                        .build())
                .headers(httpHeaders -> httpHeaders.addAll(headers))
                .retrieve()
                .bodyToMono(String.class)
                .flatMap(response -> parseOutputData(response, outputType)) // outputType 전달
                .block();
    }
}

//    // output1 데이터 파싱
//    private Mono<List<FixedIncomeDto>> parseOutputData(String response, int outputType) {
//        try {
//            List<FixedIncomeDto> resultList = new ArrayList<>();
//            JsonNode rootNode = objectMapper.readTree(response);
//            JsonNode outputNode = rootNode.get("output1");
//
//            System.out.println("jjhdebug" + outputNode);
//
//            if (outputNode != null) {
//                if (outputNode.isArray()) {
//                    // output1이 배열일 때
//                    for (JsonNode node : outputNode) {
//                        FixedIncomeDto dto = objectMapper.treeToValue(node, FixedIncomeDto.class);
//                        resultList.add(dto);
//                    }
//                } else {
//                    // output1이 객체일 때
//                    FixedIncomeDto dto = objectMapper.treeToValue(outputNode, FixedIncomeDto.class);
//                    resultList.add(dto);
//                }
//            }
//
//            return Mono.just(resultList);
//
//        } catch (Exception e) {
//            return Mono.error(new RuntimeException("output1 데이터 파싱 실패", e));
//        }
//    }
//
//    // output1 데이터 조회
//    public List<FixedIncomeDto> getOutput1Data() {
//        HttpHeaders headers = createHttpHeaders();
//
//        return webClient.get()
//                .uri(uriBuilder -> uriBuilder
//                        .path("/uapi/domestic-stock/v1/quotations/comp-interest") // 실제 API 경로로 변경
//                        .queryParam("FID_COND_MRKT_DIV_CODE", "I")
//                        .queryParam("FID_COND_SCR_DIV_CODE", "20702")
//                        .queryParam("FID_DIV_CLS_CODE", "1")
//                        .queryParam("FID_DIV_CLS_CODE1", "")
//                        .build())
//                .headers(httpHeaders -> httpHeaders.addAll(headers))
//                .retrieve()
//                .bodyToMono(String.class)
//                .flatMap(response -> parseOutputData(response, outputType))
//                .block();
//    }
//}
//
