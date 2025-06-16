package com.project.stock.investory.mainData.service;

import com.project.stock.investory.mainData.dto.StockPriceDto;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

@Service
public class StockPriceService {
//    private final WebClient webClient;

    @Value("${base-url}")
    private String baseUrl;

    @Value("${appkey}")
    private String appKey;

    @Value("${appsecret}")
    private String appSecret;

    @Value("${access_token}")
    private String accessToken;

    public Mono<StockPriceDto> getStockPrice(String iscd) {
        return WebClient.builder()
                .baseUrl(baseUrl)
                .build()
                .get()
                .uri(uriBuilder -> uriBuilder
                        .path("/uapi/domestic-stock/v1/quotations/inquire-price")
                        .queryParam("fid_cond_mrkt_div_code", "J")
                        .queryParam("fid_input_iscd", iscd)
                        .build())
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + accessToken)
                .header("appkey", appKey)
                .header("appsecret", appSecret)
                .header("tr_id", "FHKST01010100") // 실시간주식체결가
                .header("custtype", "P")
                .retrieve()
                .bodyToMono(StockPriceDto.class);
    }
}
