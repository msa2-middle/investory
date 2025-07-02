package com.project.stock.investory.stockInfo.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.project.stock.investory.stockInfo.dto.*;
import com.project.stock.investory.stockInfo.exception.StockNotFoundException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;

import java.util.ArrayList;
import java.util.List;

@Service
public class StockInfoService {
    @Value("${appkey}")
    private String appkey;

    @Value("${appsecret}")
    private String appSecret;

    @Value("${access_token}")
    private String accessToken;

    //Spring에서 HTTP 요청을 보내는 비동기 & 논블로킹 HTTP 클라이언트입니다.
    private final WebClient webClient;
    //ObjectMapper를 이용해 JSON 문자열을 **루트 노드(JsonNode 객체)**로 파싱합니다.
    private final ObjectMapper objectMapper;

    @Autowired
    public StockInfoService(WebClient.Builder webClientBuilder, ObjectMapper objectMapper) {
        this.webClient = webClientBuilder.baseUrl("https://openapi.koreainvestment.com:9443").build();
        this.objectMapper = objectMapper;
    }


    // 1. 상품 기본 조회
    private HttpHeaders createProductBasicHttpHeaders() {
        return getHttpHeaders("CTPF1604R");
    }

    public ProductBasicDTO getProductBasic(String stockId) {
        HttpHeaders headers = createProductBasicHttpHeaders();

        String response = webClient.get()
                .uri(uriBuilder -> uriBuilder.path("/uapi/domestic-stock/v1/quotations/search-info")
                        .queryParam("PDNO", stockId)
                        .queryParam("PRDT_TYPE_CD", 300)
                        .build())
                .headers(httpHeaders -> httpHeaders.addAll(headers))
                .retrieve()
                .bodyToMono(String.class)
                .block(); //동기처리

        return parseFProductBasic(response, stockId);
    }

    private ProductBasicDTO parseFProductBasic(String response, String stockId) {
        try {
            JsonNode rootNode = objectMapper.readTree(response);
            JsonNode outputNode = rootNode.get("output");

            if (outputNode == null || outputNode.isNull() || outputNode.get("prdt_name").isNull()) {
                throw new StockNotFoundException(stockId);
            }


            ProductBasicDTO dto = new ProductBasicDTO();
            dto.setPdno(stockId);
            dto.setPrdtName(outputNode.get("prdt_name").asText());
            dto.setPrdtEngName(outputNode.get("prdt_eng_name").asText());
            dto.setPrdtAbrvName(outputNode.get("prdt_abrv_name").asText());
            dto.setPrdtSaleStatCd(outputNode.get("prdt_sale_stat_cd").asText());
            dto.setPrdtRiskGradCd(outputNode.get("prdt_risk_grad_cd").asText());
            dto.setPrdtClsfName(outputNode.get("prdt_clsf_name").asText());
            dto.setIvstPrdtTypeCdName(outputNode.get("ivst_prdt_type_cd").asText());
            dto.setFrstErlmDt(outputNode.get("frst_erlm_dt").asText());

            return dto;

        } catch (Exception e) {
            throw new RuntimeException("Failed to parse response", e);
        }
    }


    // 2. 주식 기본 조회
    private HttpHeaders createStockBasicHttpHeaders() {
        return getHttpHeaders("CTPF1002R");
    }

    public StockBasicDTO getStockBasic(String stockId) {
        HttpHeaders headers = createStockBasicHttpHeaders();

        String response = webClient.get()
                .uri(uriBuilder -> uriBuilder.path("/uapi/domestic-stock/v1/quotations/search-stock-info")
                        .queryParam("PDNO", stockId)
                        .queryParam("PRDT_TYPE_CD", 300)
                        .build())
                .headers(httpHeaders -> httpHeaders.addAll(headers))
                .retrieve()
                .bodyToMono(String.class)
                .block();
        return parseFStockBasic(response, stockId);

    }

    private StockBasicDTO parseFStockBasic(String response, String stockId) {
        try {
            JsonNode rootNode = objectMapper.readTree(response);
            JsonNode outputNode = rootNode.get("output");

            if (outputNode == null) throw new IllegalStateException("output is null");


            StockBasicDTO dto = new StockBasicDTO();
            dto.setPdno(stockId);
            dto.setPrdtName(outputNode.get("prdt_name").asText());
            dto.setPrdtEngName(outputNode.get("prdt_eng_name").asText());
            dto.setMketIdCd(outputNode.get("mket_id_cd").asText());
            dto.setLstgStqt(outputNode.get("lstg_stqt").asText());
            dto.setCpta(outputNode.get("cpta").asText());
            dto.setPapr(outputNode.get("papr").asText());
            dto.setThdtClpr(outputNode.get("thdt_clpr").asText());
            dto.setBfdyClpr(outputNode.get("bfdy_clpr").asText());
            dto.setClprChngDt(outputNode.get("clpr_chng_dt").asText());
            dto.setKospi200ItemYn(outputNode.get("kospi200_item_yn").asText());
            dto.setStdIdstClsfCdName(outputNode.get("std_idst_clsf_cd_name").asText());
            dto.setIdxBztpLclsCdName(outputNode.get("idx_bztp_lcls_cd_name").asText());
            dto.setAdmnItemYn(outputNode.get("admn_item_yn").asText());
            dto.setTrStopYn(outputNode.get("tr_stop_yn").asText());

            return dto;

        } catch (Exception e) {
            throw new IllegalStateException("output is null");
        }
    }


    // 3. 대차대조표
    private HttpHeaders createBalanceSheetHttpHeaders() {
        return getHttpHeaders("FHKST66430100");
    }

    public List<BalanceSheetDTO> getBalanceSheet(String stockId) {
        HttpHeaders headers = createBalanceSheetHttpHeaders();

        String response = webClient.get()
                .uri(uriBuilder -> uriBuilder.path("/uapi/domestic-stock/v1/finance/balance-sheet")
                        .queryParam("fid_cond_mrkt_div_code", "J")
                        .queryParam("fid_input_iscd", stockId)
                        .queryParam("fid_div_cls_code", "1") //  (0:연말, 1:분기)
                        .build())
                .headers(httpHeaders -> httpHeaders.addAll(headers))
                .retrieve()
                .bodyToMono(String.class)
                .block();
        return ParseFBalanceSheet(response);
    }

    private List<BalanceSheetDTO> ParseFBalanceSheet(String response) {
        try {
            List<BalanceSheetDTO> dtoList = new ArrayList<>();
            JsonNode rootNode = objectMapper.readTree(response);
            JsonNode outputNode = rootNode.get("output");

            if (outputNode != null) {
                for (JsonNode node : outputNode) {
                    BalanceSheetDTO dto = new BalanceSheetDTO();
                    dto.setStacYymm(node.get("stac_yymm").asText());
                    dto.setCras(node.get("cras").asText());
                    dto.setFxas(node.get("fxas").asText());
                    dto.setTotalAset(node.get("total_aset").asText());
                    dto.setFlowLblt(node.get("flow_lblt").asText());
                    dto.setFixLblt(node.get("fix_lblt").asText());
                    dto.setTotalLblt(node.get("total_lblt").asText());
                    dto.setCpfn(node.get("cpfn").asText());
                    dto.setCfpSurp(node.get("cfp_surp").asText());
                    dto.setPrfiSurp(node.get("prfi_surp").asText());
                    dto.setTotalLblt(node.get("total_lblt").asText());
                    dtoList.add(dto);
                }
            }
            return dtoList;
        } catch (Exception e) {
            throw new RuntimeException("대차대조표 파싱 실패", e);
        }
    }


    // 4. 손익 계산서
    private HttpHeaders createIncomeStatementHttpHeaders() {
        return getHttpHeaders("FHKST66430200");
    }

    public List<IncomeStatementDTO> getIncomeStatement(String stockId) {
        HttpHeaders headers = createIncomeStatementHttpHeaders();

        String response = webClient.get()
                .uri(uriBuilder -> uriBuilder.path("/uapi/domestic-stock/v1/finance/income-statement")
                        .queryParam("fid_cond_mrkt_div_code", "J")
                        .queryParam("fid_input_iscd", stockId)
                        .queryParam("fid_div_cls_code", 1) // 분류 구분 코드 (0:연말, 1:분기)
                        .build())
                .headers(httpHeaders -> httpHeaders.addAll(headers))
                .retrieve()
                .bodyToMono(String.class)
                .block();
        return parseFIncomeStatement(response);
    }

    private List<IncomeStatementDTO> parseFIncomeStatement(String response) {
        try {
            List<IncomeStatementDTO> dtoList = new ArrayList<>();
            JsonNode rootNode = objectMapper.readTree(response);
            JsonNode outputNode = rootNode.get("output");

            if (outputNode != null) {
                for (JsonNode node : outputNode) {
                    IncomeStatementDTO dto = new IncomeStatementDTO();
                    dto.setStacYymm(node.get("stac_yymm").asText());
                    dto.setSaleAccount(node.get("sale_account").asText());
                    dto.setSaleCost(node.get("sale_cost").asText());
                    dto.setSaleTotlPrfi(node.get("sale_totl_prfi").asText());
                    dto.setDeprCost(node.get("depr_cost").asText());
                    dto.setSellMang(node.get("sell_mang").asText());
                    dto.setBsopPrti(node.get("bsop_prti").asText());
                    dto.setBsopPrti(node.get("bsop_prti").asText());
                    dto.setBsopNonErnn(node.get("bsop_non_ernn").asText());
                    dto.setBsopNonExpn(node.get("bsop_non_expn").asText());
                    dto.setOpPrfi(node.get("op_prfi").asText());
                    dto.setSpecPrfi(node.get("spec_prfi").asText());
                    dto.setSpecLoss(node.get("spec_loss").asText());
                    dto.setThtrNtin(node.get("thtr_ntin").asText());
                    dtoList.add(dto);
                }
            }
            return dtoList;
        } catch (Exception e) {
            throw new RuntimeException("손익계산서 파싱 실패", e);
        }
    }


    // 5. 재무비율
    private HttpHeaders createFinancialRatioHttpHeaders() {
        return getHttpHeaders("FHKST66430300");
    }

    public List<FinancialRatioDTO> getFinancialRatio(String stockId) {
        HttpHeaders headers = createFinancialRatioHttpHeaders();

        String response= webClient.get()
                .uri(uriBuilder -> uriBuilder.path("/uapi/domestic-stock/v1/finance/financial-ratio")
                        .queryParam("fid_cond_mrkt_div_code", "J")
                        .queryParam("fid_input_iscd", stockId)
                        .queryParam("fid_div_cls_code", "1") //분류 구분 코드 (0:연말, 1:분기)
                        .build())
                .headers(httpHeaders -> httpHeaders.addAll(headers))
                .retrieve()
                .bodyToMono(String.class)
                .block();
        return ParseFFinancialRatio(response);
    }

    private List<FinancialRatioDTO> ParseFFinancialRatio(String response) {
        try {
            List<FinancialRatioDTO> dtoList = new ArrayList<>();
            JsonNode rootNode = objectMapper.readTree(response);
            JsonNode outputNode = rootNode.get("output");

            if (outputNode != null) {
                for (JsonNode node : outputNode) {
                    FinancialRatioDTO dto = new FinancialRatioDTO();
                    dto.setStacYymm(node.get("stac_yymm").asText());
                    dto.setGrs(node.get("grs").asText());
                    dto.setBsopPrfiInrt(node.get("bsop_prfi_inrt").asText());
                    dto.setNtinInrt(node.get("ntin_inrt").asText());
                    dto.setRoeVal(node.get("roe_val").asText());
                    dto.setEps(node.get("eps").asText());
                    dto.setSps(node.get("sps").asText());
                    dto.setRsrvRate(node.get("rsrv_rate").asText());
                    dto.setLbltRate(node.get("lblt_rate").asText());
                    dtoList.add(dto);
                }
            }
            return dtoList;
        } catch (Exception e) {
            throw new RuntimeException("재무재표 파싱 실패");
        }
    }


    // 6. 수익성 비율 (ProfitRatioDTO)
    private HttpHeaders createProfitRatioHttpHeaders() {
        return getHttpHeaders("FHKST66430400");
    }

    public List<ProfitRatioDTO> getProfitRatio(String stockId) {
        HttpHeaders headers = createProfitRatioHttpHeaders();

        String response = webClient.get()
                .uri(uriBuilder -> uriBuilder.path("/uapi/domestic-stock/v1/finance/profit-ratio")
                        .queryParam("fid_cond_mrkt_div_code", "J")
                        .queryParam("fid_input_iscd", stockId)
                        .queryParam("fid_div_cls_code", "1") //분류 구분 코드 (0:연말, 1:분기)
                        .build())
                .headers(httpHeaders -> httpHeaders.addAll(headers))
                .retrieve()
                .bodyToMono(String.class)
                .block();
        return  ParseFProfitRatio(response);
    }

    private List<ProfitRatioDTO> ParseFProfitRatio(String response) {
        try {
            List<ProfitRatioDTO> dtoList = new ArrayList<>();
            JsonNode rootNode = objectMapper.readTree(response);
            JsonNode outputNode = rootNode.get("output");

            if (outputNode != null) {
                for (JsonNode node : outputNode) {
                    ProfitRatioDTO dto = new ProfitRatioDTO();
                    dto.setStacYymm(node.get("stac_yymm").asText());
                    dto.setCptlNtinRate(node.get("cptl_ntin_rate").asText());
                    dto.setSelfCptlNtinInrt(node.get("self_cptl_ntin_inrt").asText());
                    dto.setSaleNtinRate(node.get("sale_ntin_rate").asText());
                    dto.setSaleTotlRate(node.get("sale_totl_rate").asText());
                    dtoList.add(dto);
                }
            }
            return dtoList;
        } catch (Exception e) {
            throw  new RuntimeException("수익성 비율 파싱 실패");
        }
    }


    // 7. 안정성 비율 StabilityRatioDTO
    private HttpHeaders createStabilityRatioHttpHeaders() {
        return getHttpHeaders("FHKST66430600");
    }

    public List<StabilityRatioDTO> getStabilityRatio(String stcokId) {
        HttpHeaders headers = createStabilityRatioHttpHeaders();

        String response = webClient.get()
                .uri(uriBuilder -> uriBuilder.path("uapi/domestic-stock/v1/finance/stability-ratio")
                        .queryParam("fid_cond_mrkt_div_code", "J")
                        .queryParam("fid_input_iscd", stcokId)
                        .queryParam("fid_div_cls_code", "1") //분류 구분 코드 (0:연말, 1:분기)
                        .build())
                .headers(httpHeaders -> httpHeaders.addAll(headers))
                .retrieve()
                .bodyToMono(String.class)
                .block();
        return ParseFStabilityRatio(response);
    }

    private List<StabilityRatioDTO> ParseFStabilityRatio(String response) {
        try {
            List<StabilityRatioDTO> dtoList = new ArrayList<>();
            JsonNode rootNode = objectMapper.readTree(response);
            JsonNode outputNode = rootNode.get("output");
            if (outputNode != null) {
                for (JsonNode node : outputNode) {
                    StabilityRatioDTO dto = new StabilityRatioDTO();
                    dto.setStacYymm(node.get("stac_yymm").asText());
                    dto.setLbltRate(node.get("lblt_rate").asText());
                    dto.setBramDepn(node.get("bram_depn").asText());
                    dto.setCrntRate(node.get("crnt_rate").asText());
                    dto.setQuckRate(node.get("quck_rate").asText());
                    dtoList.add(dto);
                }
            }
            return dtoList;
        } catch (Exception e) {
            throw  new RuntimeException("안전성 비율 파싱 실패");
        }
    }


    // 8. 성장성 비율 GrowthRatioDTO
    private HttpHeaders createGrowthRatioHttpHeaders() {
        return getHttpHeaders("FHKST66430800");
    }

    public List<GrowthRatioDTO> getGrowthRatio(String stockId) {
        HttpHeaders headers = createGrowthRatioHttpHeaders();

        String response= webClient.get()
                .uri(uriBuilder -> uriBuilder.path("/uapi/domestic-stock/v1/finance/growth-ratio")
                        .queryParam("fid_cond_mrkt_div_code", "J")
                        .queryParam("fid_input_iscd", stockId)
                        .queryParam("fid_div_cls_code", "1") //분류 구분 코드 (0:연말, 1:분기)
                        .build())
                .headers(httpHeaders -> httpHeaders.addAll(headers))
                .retrieve()
                .bodyToMono(String.class)
                .block();
        return ParseFGrowthRatio(response);

    }

    private List<GrowthRatioDTO> ParseFGrowthRatio(String response) {
        try {
            List<GrowthRatioDTO> dtoList = new ArrayList<>();
            JsonNode rootNode = objectMapper.readTree(response);
            JsonNode outputNode = rootNode.get("output");
            if (outputNode != null) {
                for (JsonNode node : outputNode) {
                    GrowthRatioDTO dto = new GrowthRatioDTO();
                    dto.setStacYymm(node.get("stac_yymm").asText());
                    dto.setGrs(node.get("grs").asText());
                    dto.setBsopPrfiInrt(node.get("bsop_prfi_inrt").asText());
                    dto.setEqutInrt(node.get("equt_inrt").asText());
                    dto.setTotlAsetInrt(node.get("totl_aset_inrt").asText());
                    dtoList.add(dto);
                }
            }
            return dtoList;
        } catch (Exception e) {
            throw new RuntimeException("성장성 비율 파싱 실패");
        }
    }



    // 헤더 생성 메서드
    private HttpHeaders getHttpHeaders(String trId) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.setBearerAuth(accessToken);
        headers.set("appkey", appkey);
        headers.set("appSecret", appSecret);
        headers.set("custtype", "P");
        headers.set("tr_id", trId);
        return headers;
    }
}

