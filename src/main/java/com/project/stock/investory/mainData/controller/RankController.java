package com.project.stock.investory.mainData.controller;

import com.project.stock.investory.mainData.dto.RankDto;
import com.project.stock.investory.mainData.service.RankService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Mono;

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
     * 페이징 처리
     */
    // 거래량
    @GetMapping("/volume-rank")
    public Mono<Page<RankDto>> getVolumeRank(@RequestParam(defaultValue = "0") int page,
                                             @RequestParam(defaultValue = "10") int size) {
        String option = "1";
        Pageable pageable = PageRequest.of(page, size);
        return rankService.getRankDataWithPagination(option, pageable);
    }

    // 거래 대금
    @GetMapping("/trading-value-rank")
    public Mono<Page<RankDto>> getTradingValueRank(@RequestParam(defaultValue = "0") int page,
                                                   @RequestParam(defaultValue = "10") int size) {
        String option = "2";
        Pageable pageable = PageRequest.of(page, size);
        return rankService.getRankDataWithPagination(option, pageable);
    }

    // 급상승
    @GetMapping("/price-up-rank")
    public Mono<Page<RankDto>> getPriceUpRank(@RequestParam(defaultValue = "0") int page,
                                              @RequestParam(defaultValue = "10") int size) {
        String option = "3";
        Pageable pageable = PageRequest.of(page, size);
        return rankService.getRankDataWithPagination(option, pageable);
    }

    //급하락
    @GetMapping("/price-down-rank")
    public Mono<Page<RankDto>> getPriceDownRank(@RequestParam(defaultValue = "0") int page,
                                                @RequestParam(defaultValue = "10") int size) {
        String option = "4";
        Pageable pageable = PageRequest.of(page, size);
        return rankService.getRankDataWithPagination(option, pageable);
    }

    // 시총
    @GetMapping("/market-cap-rank")
    public Mono<Page<RankDto>> getMarketCapRank(@RequestParam(defaultValue = "0") int page,
                                                @RequestParam(defaultValue = "10") int size) {
        String option = "5";
        Pageable pageable = PageRequest.of(page, size);
        return rankService.getRankDataWithPagination(option, pageable);
    }

}
