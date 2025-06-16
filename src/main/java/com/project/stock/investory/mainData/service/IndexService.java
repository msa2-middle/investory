package com.project.stock.investory.mainData.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.project.stock.investory.mainData.dto.IndexDto;
import jakarta.persistence.Index;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

@Service
public class IndexService {

    @Value("${appkey}")
    private String appKey;

    @Value("${appsecret}")
    private String appSecret;

    @Value("${access_token}")
    private String accessToken;

//    private final ObjectMapper objectMapper = new ObjectMapper();


    //==========================================================

    //  WebClient: 비동기 HTTP 클라이언트로 KIS API 호출에 사용
    //  ObjectMapper: JSON 응답을 객체로 변환하는 Jackson 라이브러리 컴포넌트
    private final WebClient webClient;
    private final ObjectMapper objectMapper;

    @Autowired
    public IndexService(WebClient.Builder webClientBuilder, ObjectMapper objectMapper) {
        this.webClient = webClientBuilder.baseUrl("https://openapi.koreainvestment.com:9443").build();
        this.objectMapper = objectMapper;
    }

    //  HTTP 헤더 생성
    private HttpHeaders createIndexHttpHeaders() {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.setBearerAuth(accessToken);
        headers.set("appkey", appKey);
        headers.set("appSecret", appSecret);
        headers.set("tr_id", "FHKUP03500100");
        headers.set("custtype", "P");
        return headers;
    }


    //  API 응답 파싱
    private List<IndexDto> parseIndex(String response) {
        try {
            List<IndexDto> responseDataList = new ArrayList<>();
            JsonNode rootNode = objectMapper.readTree(response);
            JsonNode outputNode = rootNode.get("output1");

            System.out.println("debug jjh" + outputNode);

            if (outputNode != null) {
                IndexDto dto = objectMapper.treeToValue(outputNode, IndexDto.class);
                responseDataList.add(dto);
//                for (JsonNode node : outputNode) {
//                    // 자동 mapping
//                    IndexDto dto = objectMapper.treeToValue(node, IndexDto.class);
//                    responseDataList.add(dto);
//                }
                System.out.println(dto);
            }

            return responseDataList;

        } catch (Exception e) {
            throw new RuntimeException("index 데이터 파싱 실패", e);
        }
    }

    // get data
    public List<IndexDto> getIndices(String option) {

        HttpHeaders headers = createIndexHttpHeaders();
        String today = LocalDate.now().format(DateTimeFormatter.BASIC_ISO_DATE);

        System.out.println("debugjjh");

        String response = webClient.get()
                .uri(uriBuilder -> uriBuilder
                        .path("/uapi/domestic-stock/v1/quotations/inquire-daily-indexchartprice")
                        .queryParam("FID_COND_MRKT_DIV_CODE", "U")
                        .queryParam("FID_INPUT_ISCD", option)
                        .queryParam("FID_INPUT_DATE_1", today)
                        .queryParam("FID_INPUT_DATE_2", today)
                        .queryParam("FID_PERIOD_DIV_CODE", "D")
                        .build())
                .headers(httpHeaders -> httpHeaders.addAll(headers))
                .retrieve()
                .bodyToMono(String.class)
                .block();

        System.out.println("debugjjh" + response);

        return parseIndex(response);
    }


    //==========================================================


//
//
//    // 지수 데이터 조회 메서드
//    public List<IndexDto> getIndices(String option) {
//
//        String today = LocalDate.now().format(DateTimeFormatter.BASIC_ISO_DATE);
//
//        // KIS API로 HTTP GET 요청 (WebClient 사용)
//        String response = WebClient.builder()
//                .baseUrl(baseUrl)
//                .build()
//                .get()
//                .uri(uriBuilder -> uriBuilder
//                        .path("/uapi/domestic-stock/v1/quotations/inquire-daily-indexchartprice")
//                        .queryParam("FID_COND_MRKT_DIV_CODE", "U")
//                        .queryParam("FID_INPUT_ISCD", option)
//                        .queryParam("FID_INPUT_DATE_1", today)
//                        .queryParam("FID_INPUT_DATE_2", today)
//                        .queryParam("FID_PERIOD_DIV_CODE", "D")
//                        .build())
//                // 필수 헤더 설정
//                .header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
//                .header(HttpHeaders.AUTHORIZATION, "Bearer " + accessToken)
//                .header("appkey", appKey)
//                .header("appsecret", appSecret)
//                .header("tr_id", "FHKUP03500100")
//                .header("custtype", "P")
//                .retrieve()
//                .bodyToMono(String.class)
//                .block();
//
//        try {
//            JsonNode root = objectMapper.readTree(response);
//            List<IndexDto> result = new ArrayList<>();
//            // output1 처리 (단일 객체)
//            JsonNode output1 = root.path("output1");
//
//            if (output1.isObject() && !output1.isEmpty()) {
//                IndexDto dto = objectMapper.treeToValue(output1, IndexDto.class);
//                result.add(dto);
//            }
//
//            return result;
//
//        } catch (Exception e) {throw new RuntimeException("KOSPI 데이터 파싱 실패", e);}
//
//    }

}
