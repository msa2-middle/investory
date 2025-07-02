package com.project.stock.investory.mainData.controller;

import com.project.stock.investory.mainData.dto.FixedIncomeDto;
import com.project.stock.investory.mainData.service.FixedIncomeService;
import io.swagger.v3.oas.annotations.Operation;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/main")
@RequiredArgsConstructor
public class FixedIncomeController {

    private final FixedIncomeService fixedIncomeService;


    @Operation(summary = "output1(해외 채권/금리) 데이터 조회")
    @GetMapping("/fixed-income/foreign")
    public ResponseEntity<List<FixedIncomeDto>> getOutput1Data() {
        List<FixedIncomeDto> data = fixedIncomeService.getOutputData(1);
        return ResponseEntity.ok(data);
    }

    @Operation(summary = "output2(국내 채권/금리) 데이터 조회")
    @GetMapping("/fixed-income/domestic")
    public ResponseEntity<List<FixedIncomeDto>> getOutput2Data() {
        List<FixedIncomeDto> data = fixedIncomeService.getOutputData(2);
        return ResponseEntity.ok(data);
    }

}