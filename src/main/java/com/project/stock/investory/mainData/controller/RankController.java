package com.project.stock.investory.mainData.controller;

import com.project.stock.investory.mainData.dto.RankDto;
import com.project.stock.investory.mainData.service.RankService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Mono;

import java.util.List;

@RestController
@RequestMapping("/main")
public class RankController {

    private final RankService rankService;

    @Autowired
    public RankController(RankService volumeRankService) {
        this.rankService = volumeRankService;
    }

    /**
     * Mono<List<T>>를 반환하면 Spring WebFlux가 자동으로 200 OK로 응답
     */
    // 거래량
    @GetMapping("/volume-rank")
    public Mono<List<RankDto>> getVolumeRank() {
        String option = "1";
        return rankService.getRankData(option);
    }
    // 거래 대금
    @GetMapping("/trading-value-rank")
    public Mono<List<RankDto>> getTradingValueRank() {
        String option = "2";
        return rankService.getRankData(option);
    }
    // 급상승
    @GetMapping("/price-up-rank")
    public Mono<List<RankDto>> getPriceUpRank() {
        String option = "3";
        return rankService.getRankData(option);
    }
    //급하락
    @GetMapping("/price-down-rank")
    public Mono<List<RankDto>> getPriceDownRank() {
        String option = "4";
        return rankService.getRankData(option);
    }
    // 시총
    @GetMapping("/market-cap-rank")
    public Mono<List<RankDto>> getMarketCapRank() {
        String option = "5";
        return rankService.getRankData(option);
    }

}
