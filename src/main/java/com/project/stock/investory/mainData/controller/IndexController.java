package com.project.stock.investory.mainData.controller;

import com.project.stock.investory.mainData.dto.IndexDto;
import com.project.stock.investory.mainData.service.IndexService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Mono;

import java.util.List;

@RestController
@RequestMapping("/main")
@RequiredArgsConstructor
public class IndexController {

    private final IndexService indexService;

    /**
     * Mono<List<T>>를 반환하면 Spring WebFlux가 자동으로 200 OK로 응답
     */
    @GetMapping("/kospi")
    public Mono<List<IndexDto>> getKospi() {
        String option = "0001";
        return indexService.getIndexData(option);
    }

    @GetMapping("/kosdaq")
    public Mono<List<IndexDto>> getKosdaq() {
        String option = "1001";
        return indexService.getIndexData(option);
    }

    @GetMapping("/kospi200")
    public Mono<List<IndexDto>> getKospi200() {
        String option = "2001";
        return indexService.getIndexData(option);
    }

}