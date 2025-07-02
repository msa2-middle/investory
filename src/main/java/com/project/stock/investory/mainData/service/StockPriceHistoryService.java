package com.project.stock.investory.mainData.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.project.stock.investory.mainData.dto.StockPriceHistoryDto;
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
public class StockPriceHistoryService {

    // 인증 정보
    @Value("${appkey}")
    private String appkey;

    @Value("${appsecret}")
    private String appSecret;

    @Value("${access_token}")
    private String accessToken;

    private final WebClient webClient;
    private final ObjectMapper objectMapper;

    @Autowired
    public StockPriceHistoryService(WebClient.Builder webClientBuilder, ObjectMapper objectMapper) {
        this.webClient = webClientBuilder.baseUrl("https://openapi.koreainvestment.com:9443").build();
        this.objectMapper = objectMapper;
    }

    // HTTP 헤더 생성
    private HttpHeaders createHistoryHeaders() {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.setBearerAuth(accessToken);
        headers.set("appkey", appkey);
        headers.set("appsecret", appSecret);
        headers.set("tr_id", "FHKST03010100"); // 주식현재가 시세 TR ID
        return headers;
    }

    /**
     * 주식 가격 이력 조회(history)
     *
     * @param stockId 종목코드 (예: "005930")
     * @param period  기간 타입 (D:일봉, W:주봉, M:월봉)
     * @return StockPriceHistroyDto 리스트
     */
    public List<StockPriceHistoryDto> getStockPriceHistory(
            String stockId,
            String period,
            String periodDiv
    ) {
        HttpHeaders headers = createHistoryHeaders();

        return webClient.get()
                .uri(uriBuilder -> uriBuilder
                        .path("/uapi/domestic-stock/v1/quotations/inquire-daily-itemchartprice")
                        .queryParam("fid_cond_mrkt_div_code", "J") // J: 주식
                        .queryParam("fid_input_iscd", stockId)
                        .queryParam("fid_input_date_1", "20000101") // 가장 과거 데이터 하한 설정
                        .queryParam("fid_input_date_2", period)
                        .queryParam("fid_period_div_code", periodDiv) //  (D:일봉, W:주봉, M:월봉, Y:년봉)
                        .queryParam("fid_org_adj_prc", "0") //  (0:수정주가 1:원주가)
                        .build())
                .headers(httpHeaders -> httpHeaders.addAll(headers))
                .retrieve()
                .bodyToMono(String.class)
                .flatMap(this::parseHistoryData)
                .block();
    }

    // 응답 데이터 파싱
    private Mono<List<StockPriceHistoryDto>> parseHistoryData(String response) {
        try {
            JsonNode rootNode = objectMapper.readTree(response);
            JsonNode outputNode = rootNode.get("output2");

            List<StockPriceHistoryDto> historyList = new ArrayList<>();

            if (outputNode != null && outputNode.isArray()) {
                for (JsonNode node : outputNode) {
                    StockPriceHistoryDto dto = objectMapper.treeToValue(node, StockPriceHistoryDto.class);
                    historyList.add(dto);
                }
            }

            return Mono.just(historyList);

        } catch (Exception e) {
            return Mono.error(new RuntimeException("주가 이력 데이터 파싱 실패", e));
        }
    }
}
