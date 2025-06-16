package com.project.stock.investory.mainData.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.project.stock.investory.mainData.dto.IndexDto;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

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
    private Mono<List<IndexDto>> parseIndexData(String response) {
        try {
            List<IndexDto> responseDataList = new ArrayList<>();
            JsonNode rootNode = objectMapper.readTree(response);
            JsonNode outputNode = rootNode.get("output1");
            JsonNode outputNode2 = rootNode.get("output2");

            // output1에는 현재 가격 및 상세 데이터 들어감
            if (outputNode != null) {
                IndexDto dto = objectMapper.treeToValue(outputNode, IndexDto.class);
                responseDataList.add(dto);
                System.out.println(dto);
            }

            // output2에는 과거 일자별 데이터(30영업일) 들어감 -> 사용하지 않음
            if (outputNode2 != null) {
                System.out.println("debug jjh" + rootNode);
            }

            return Mono.just(responseDataList);

        } catch (Exception e) {
            return Mono.error(new RuntimeException("index 데이터 파싱 실패", e));
        }
    }

    // get data
    public Mono<List<IndexDto>> getIndexData(String option) {

        HttpHeaders headers = createIndexHttpHeaders();
        String today = LocalDate.now().format(DateTimeFormatter.BASIC_ISO_DATE);

        return webClient.get()
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
                .flatMap(this::parseIndexData);
    }

}
