package com.project.stock.investory.mainData.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.project.stock.investory.mainData.dto.RankDto;
import com.project.stock.investory.stockInfo.dto.StockApiResponseDTO;
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
public class RankService {
    // 필드 주입
    @Value("${appkey}")
    private String appkey;

    @Value("${appsecret}")
    private String appSecret;

    @Value("${access_token}")
    private String accessToken;

    //  WebClient: 비동기 HTTP 클라이언트로 KIS API 호출에 사용
    //  ObjectMapper: JSON 응답을 객체로 변환하는 Jackson 라이브러리 컴포넌트
    private final WebClient webClient;
    private final ObjectMapper objectMapper;

    @Autowired
    public RankService(WebClient.Builder webClientBuilder, ObjectMapper objectMapper) {
        this.webClient = webClientBuilder.baseUrl("https://openapi.koreainvestment.com:9443").build();
        this.objectMapper = objectMapper;
    }

    //  HTTP 헤더 생성
    private HttpHeaders createRankHttpHeaders() {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.setBearerAuth(accessToken);
        headers.set("appkey", appkey);
        headers.set("appSecret", appSecret);
        headers.set("tr_id", "HHKST03900400");
        headers.set("custtype", "P");
        return headers;
    }

    //  API 응답 파싱 (parseFVolumeRank)
    private Mono<List<RankDto>> parseRankData(String response) {
        try {
            List<RankDto> responseDataList = new ArrayList<>();
            JsonNode rootNode = objectMapper.readTree(response);
            JsonNode outputNode = rootNode.get("output2");

            if (outputNode != null) {
                for (JsonNode node : outputNode) {

                    // 자동 mapping
                    RankDto dto = objectMapper.treeToValue(node, RankDto.class);
                    responseDataList.add(dto);
                }
            }

            return Mono.just(responseDataList);

        } catch (Exception e) {
            return Mono.error(new RuntimeException("Rank 데이터 파싱 실패", e));
        }
    }

    // get data
    public Mono<List<RankDto>> getRankData(String option) {

        HttpHeaders headers = createRankHttpHeaders();

        return webClient.get()
                .uri(uriBuilder -> uriBuilder
                        .path("/uapi/domestic-stock/v1/ranking/fluctuation")
                        .queryParam("user_id", "jjgus29")
                        .queryParam("seq", option)
                        .build())
                .headers(httpHeaders -> httpHeaders.addAll(headers))
                .retrieve()
                .bodyToMono(String.class)
                .flatMap(this::parseRankData);
    }


    // 디비에 저장하기 위해 필요 (StockApiResponseDTO로 변환하기 위해)
    public Mono<List<StockApiResponseDTO>> getStockIdAndNameOnly(String option) {
        return getRankData(option) // 기존 RankDto 리스트 반환하는 내부 메서드
                .map(rankList -> rankList.stream()
                        .map(rank -> new StockApiResponseDTO(rank.getCode(), rank.getName()))
                        .toList()
                );
    }
}

