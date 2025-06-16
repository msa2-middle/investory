package com.project.stock.investory.mainData.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.project.stock.investory.mainData.dto.RankDto;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;

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
    private HttpHeaders createVolumeRankHttpHeaders() {
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
    private List<RankDto> parseRank(String response) {
        try {
            List<RankDto> responseDataList = new ArrayList<>();
            JsonNode rootNode = objectMapper.readTree(response);
            System.out.println("rootNodejjgus29" + rootNode);
            JsonNode outputNode = rootNode.get("output2");
            if (outputNode != null) {
                for (JsonNode node : outputNode) {
                    RankDto responseData = new RankDto();
                    responseData.setCode(node.get("code").asText());
                    responseData.setName(node.get("name").asText());
                    responseData.setDaebi(node.get("daebi").asText());
                    responseData.setPrice(node.get("price").asText());
                    responseData.setChgrate(node.get("chgrate").asText());
                    responseData.setAcmlVol(node.get("acml_vol").asText());
                    responseData.setTradeAmt(node.get("trade_amt").asText());
                    responseData.setChange(node.get("change").asText());
                    responseData.setCttr(node.get("cttr").asText());
                    responseData.setOpen(node.get("open").asText());
                    responseData.setHigh(node.get("high").asText());
                    responseData.setLow(node.get("low").asText());
                    responseData.setHigh52(node.get("high52").asText());
                    responseData.setLow52(node.get("low52").asText());
                    responseData.setExpPrice(node.get("expprice").asText());
                    responseData.setExpChange(node.get("expchange").asText());
                    responseData.setExpChgGrate(node.get("expchggrate").asText());
                    responseData.setExpCVol(node.get("expcvol").asText());
                    responseData.setChgRate2(node.get("chgrate2").asText());
                    responseData.setExpDaebi(node.get("expdaebi").asText());
                    responseData.setRecPrice(node.get("recprice").asText());
                    responseData.setUplmtPrice(node.get("uplmtprice").asText());
                    responseData.setDnlmtPrice(node.get("dnlmtprice").asText());
                    responseData.setStotPrice(node.get("stotprice").asText());
                    responseDataList.add(responseData);
                }
            }

            return responseDataList;

        } catch (Exception e) {
            throw new IllegalStateException("null");
        }
    }

    public List<RankDto> getRank(String option) {

        HttpHeaders headers = createVolumeRankHttpHeaders();

        String response = webClient.get()
                .uri(uriBuilder -> uriBuilder
                        .path("/uapi/domestic-stock/v1/ranking/fluctuation")
                        .queryParam("user_id", "jjgus29")
                        .queryParam("seq", option)
                        .build())
                .headers(httpHeaders -> httpHeaders.addAll(headers))
                .retrieve()
                .bodyToMono(String.class)
                .block();

        return parseRank(response);
    }

}

