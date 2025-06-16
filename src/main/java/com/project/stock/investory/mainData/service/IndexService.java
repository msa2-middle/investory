package com.project.stock.investory.mainData.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.project.stock.investory.mainData.dto.IndexDto;
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
    @Value("${base-url}")
    private String baseUrl;

    @Value("${appkey}")
    private String appKey;

    @Value("${appsecret}")
    private String appSecret;

    @Value("${access_token}")
    private String accessToken;

    private final ObjectMapper objectMapper = new ObjectMapper();



    // 지수 데이터 조회 메서드
    public List<IndexDto> getIndices(String option) {

        String today = LocalDate.now().format(DateTimeFormatter.BASIC_ISO_DATE);

        // KIS API로 HTTP GET 요청 (WebClient 사용)
        String response = WebClient.builder()
                .baseUrl(baseUrl)
                .build()
                .get()
                .uri(uriBuilder -> uriBuilder
                        .path("/uapi/domestic-stock/v1/quotations/inquire-daily-indexchartprice")
                        .queryParam("FID_COND_MRKT_DIV_CODE", "U")
                        .queryParam("FID_INPUT_ISCD", option)
                        .queryParam("FID_INPUT_DATE_1", today)
                        .queryParam("FID_INPUT_DATE_2", today)
                        .queryParam("FID_PERIOD_DIV_CODE", "D")
                        .build())
                // 필수 헤더 설정
                .header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + accessToken)
                .header("appkey", appKey)
                .header("appsecret", appSecret)
                .header("tr_id", "FHKUP03500100")
                .header("custtype", "P")
                .retrieve()
                .bodyToMono(String.class)
                .block();


        try {
            JsonNode root = objectMapper.readTree(response);
            System.out.println("JsonModeDebug: " + root);
            List<IndexDto> result = new ArrayList<>();
            // 1. output1 처리 (단일 객체)
            JsonNode output1 = root.path("output1");

            if (output1.isObject() && !output1.isEmpty()) {
                IndexDto dto = objectMapper.treeToValue(output1, IndexDto.class);
                System.out.println("DTO Debug: " + dto); // 디버깅 출력
                result.add(dto);
            }

            return result;

        } catch (Exception e) {
            throw new RuntimeException("KOSPI 데이터 파싱 실패", e);
        }
    }


}
