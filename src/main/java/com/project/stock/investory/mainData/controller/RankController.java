package com.project.stock.investory.mainData.controller;

import com.project.stock.investory.mainData.dto.RankDto;
import com.project.stock.investory.mainData.service.RankService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/main")
public class RankController {

    private RankService rankService;

    @Autowired
    public RankController(RankService volumeRankService) {
        this.rankService = volumeRankService;
    }

    @GetMapping("/volume-rank")
    public List<RankDto> getVolumeRank() {
        String option = "1";
        return rankService.getRank(option);
    }

    @GetMapping("/trading-value-rank")
    public List<RankDto> getTradingValueRank() {
        String option = "2";
        return rankService.getRank(option);
    }

    @GetMapping("/price-up-rank")
    public List<RankDto> getPriceUpRank() {
        String option = "3";
        return rankService.getRank(option);
    }

    @GetMapping("/price-down-rank")
    public List<RankDto> getPriceDownRank() {
        String option = "4";
        return rankService.getRank(option);
    }

    @GetMapping("/market-cap-rank")
    public List<RankDto> getMarketCapRank() {
        String option = "5";
        return rankService.getRank(option);
    }



}
