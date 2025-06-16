package com.project.stock.investory.mainData.controller;

import com.project.stock.investory.mainData.dto.IndexDto;
import com.project.stock.investory.mainData.service.IndexService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/main")
@RequiredArgsConstructor
public class IndexController {

    private final IndexService indexService;

    @GetMapping("/kospi")
    public ResponseEntity<List<IndexDto>> getKospi() {
        String option = "0001";
        List<IndexDto> result = indexService.getIndices(option);
        return ResponseEntity.ok(result);
    }

    @GetMapping("/kosdaq")
    public List<IndexDto> getKosdaq() {
        String option = "1001";
        return indexService.getIndices(option);
    }

    @GetMapping("/kospi200")
    public List<IndexDto> getKospi200() {
        String option = "2001";
        return indexService.getIndices(option);
    }

}