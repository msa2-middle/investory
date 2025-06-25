package com.project.stock.investory.stockAlertSetting.controller;

import com.project.stock.investory.security.CustomUserDetails;
import com.project.stock.investory.stockAlertSetting.dto.StockAlertSettingCreateRequestDTO;
import com.project.stock.investory.stockAlertSetting.dto.StockAlertSettingResponseDTO;
import com.project.stock.investory.stockAlertSetting.dto.StockAlertSettingUpdateRequestDTO;
import com.project.stock.investory.stockAlertSetting.service.StockAlertSettingService;
import com.project.stock.investory.user.dto.UserResponseDto;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/stock-alert-setting")
@RequiredArgsConstructor // final 또는 @NonNull 필드를 매개변수로 받는 생성자를 자동 생성해주는 기능
public class StockAlertSettingController {

    private final StockAlertSettingService stockAlertSettingService;

    // 주가 알람 설정 생성
    @PostMapping("/stocks/{stockId}")
    public ResponseEntity<StockAlertSettingResponseDTO> create(
            @PathVariable String stockId,
            @RequestBody StockAlertSettingCreateRequestDTO request,
            @AuthenticationPrincipal CustomUserDetails userDetails
    ) {
        StockAlertSettingResponseDTO response = stockAlertSettingService.create(stockId, request, userDetails);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    // 특정 유저의 설정 전체 조회
    @GetMapping("/")
    public ResponseEntity<List<StockAlertSettingResponseDTO>> getUserSettings(
            @AuthenticationPrincipal CustomUserDetails userDetails
    ) {

        List<StockAlertSettingResponseDTO> response =
                stockAlertSettingService.getUserSettings(userDetails);

        return ResponseEntity.ok(response);
    }

    // 특정 유저의 특정 주식 설정 조회
    @GetMapping("/stocks/{stockId}")
    public ResponseEntity<StockAlertSettingResponseDTO> getUserStockSetting(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @PathVariable String stockId
    ) {

        StockAlertSettingResponseDTO response =
                stockAlertSettingService.getUserStockSettings(userDetails, stockId);

        return ResponseEntity.ok(response);
    }

    // 특정유저의 특정 주식에 대한 설정 수정
    @PutMapping("/stocks/{stockId}")
    public ResponseEntity<StockAlertSettingResponseDTO> updateSetting(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @PathVariable String stockId,
            @RequestBody StockAlertSettingUpdateRequestDTO request
    ) {

        StockAlertSettingResponseDTO response =
                stockAlertSettingService.updateSetting(userDetails, stockId, request);
        return ResponseEntity.ok(response);
    }

    // 특정유저의 특정 주식에 대한 설정 삭제
    @DeleteMapping("/stocks/{stockId}")
    public ResponseEntity<StockAlertSettingResponseDTO> deleteSetting(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @PathVariable String stockId
    ) {

        StockAlertSettingResponseDTO response =
                stockAlertSettingService.deleteSetting(userDetails, stockId);
        return ResponseEntity.ok(response);
    }

}
