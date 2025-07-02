package com.project.stock.investory.stockAlertSetting.service;


import com.project.stock.investory.comment.exception.AuthenticationRequiredException;
import com.project.stock.investory.stockAlertSetting.exception.StockAlertSettingDuplicateKeyException;
import com.project.stock.investory.stockAlertSetting.exception.StockAlertSettingNotFoundException;
import com.project.stock.investory.stockAlertSetting.exception.UserNotFoundException;
import com.project.stock.investory.security.CustomUserDetails;
import com.project.stock.investory.stockAlertSetting.dto.StockAlertSettingCreateRequestDTO;
import com.project.stock.investory.stockAlertSetting.dto.StockAlertSettingResponseDTO;
import com.project.stock.investory.stockAlertSetting.dto.StockAlertSettingUpdateRequestDTO;
import com.project.stock.investory.stockAlertSetting.model.StockAlertSetting;
import com.project.stock.investory.stockAlertSetting.processor.StockPriceProcessor;
import com.project.stock.investory.stockAlertSetting.repository.StockAlertSettingRepository;
import com.project.stock.investory.stockInfo.model.Stock;
import com.project.stock.investory.stockInfo.repository.StockRepository;
import com.project.stock.investory.user.entity.User;
import com.project.stock.investory.user.repository.UserRepository;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class StockAlertSettingService {

    private final StockAlertSettingRepository stockAlertSettingRepository;
    private final UserRepository userRepository;
    private final StockRepository stockRepository;
    private final StockPriceProcessor stockPriceProcessor;

    // 🔥 주가 알람 설정 생성 (수정됨)
    public StockAlertSettingResponseDTO create(String stockId, StockAlertSettingCreateRequestDTO request, CustomUserDetails userDetails) {

        if (userDetails == null || userDetails.getUserId() == null) {
            throw new AuthenticationRequiredException();
        }

        User user = userRepository.findById(userDetails.getUserId())
                .orElseThrow(() -> new UserNotFoundException()); // 예외처리

        Stock stock = stockRepository.findById(stockId)
                .orElseThrow(() -> new EntityNotFoundException()); // 예외처리

        Optional<StockAlertSetting> existedSetting =
                stockAlertSettingRepository.findByUserUserIdAndStockStockId(userDetails.getUserId(), stockId);

        if (existedSetting.isPresent()) {
            throw new StockAlertSettingDuplicateKeyException();
        }

        StockAlertSetting stockAlertSetting =
                StockAlertSetting
                        .builder()
                        .user(user)
                        .stock(stock)
                        .targetPrice(request.getTargetPrice())
                        .condition(request.getCondition())
                        .createdAt(LocalDateTime.now())
                        .updatedAt(LocalDateTime.now())
                        .build();

        StockAlertSetting savedStockAlertSetting = stockAlertSettingRepository.save(stockAlertSetting);

        // 🔥 실시간 알람 조건 추가 (기존 updateStockAlertCondition을 addCondition으로 변경)
        stockPriceProcessor.addCondition(savedStockAlertSetting);

        return StockAlertSettingResponseDTO
                .builder()
                .userId(savedStockAlertSetting.getUser().getUserId())
                .stockId(savedStockAlertSetting.getStock().getStockId())
                .targetPrice(savedStockAlertSetting.getTargetPrice())
                .condition(savedStockAlertSetting.getCondition())
                .build();

    }


    // 특정 유저의 설정 전체 조회
    public List<StockAlertSettingResponseDTO> getUserSettings(CustomUserDetails userDetails) {

        if (userDetails == null || userDetails.getUserId() == null) {
            // 로그인 안 한 사용자면 빈 리스트 반환
            return List.of();
        }

        User user = userRepository.findById(userDetails.getUserId())
                .orElseThrow(()->new UserNotFoundException());

        List<StockAlertSetting> settings = stockAlertSettingRepository.findByUserUserId(user.getUserId());

        if (settings.isEmpty()) {
            return List.of();
        }

        return settings.stream()
                .map(setting -> StockAlertSettingResponseDTO.builder()
                        .userId(setting.getUser().getUserId())
                        .stockId(setting.getStock().getStockId())
                        .targetPrice(setting.getTargetPrice())
                        .condition(setting.getCondition())
                        .createdAt(setting.getCreatedAt())
                        .build())
                .collect(Collectors.toList());
    }

    // 특정 유저의 특정 주식 설정 조회
    public StockAlertSettingResponseDTO getUserStockSettings(CustomUserDetails userDetails, String stockId) {

        if (userDetails == null || userDetails.getUserId() == null) {
            throw new AuthenticationRequiredException();
        }

        User user = userRepository.findById(userDetails.getUserId())
                .orElseThrow(()->new UserNotFoundException());

        Optional<StockAlertSetting> opt =
                stockAlertSettingRepository.findByUserUserIdAndStockStockId(user.getUserId(), stockId);

        if (opt.isEmpty()) {
            return null;
        }

        StockAlertSetting setting = opt.get();

        return StockAlertSettingResponseDTO.builder()
                .userId(setting.getUser().getUserId())
                .stockId(setting.getStock().getStockId())
                .targetPrice(setting.getTargetPrice())
                .condition(setting.getCondition())
                .createdAt(setting.getCreatedAt())
                .build();
    }

    // 🔥 특정 유저의 특정 주식 설정 수정 (수정됨)
    // 얘는 마이페이지로 갈 기능이니까 굳이 다른 사람이 수정하거나 할 기회가 없으니 예외처리할 필요가 있을까..
    public StockAlertSettingResponseDTO updateSetting(
            CustomUserDetails userDetails, String stockId, StockAlertSettingUpdateRequestDTO request
    ) {
        if (userDetails == null || userDetails.getUserId() == null) {
            throw new AuthenticationRequiredException();
        }

        User user = userRepository.findById(userDetails.getUserId())
                .orElseThrow(()->new UserNotFoundException());

        StockAlertSetting setting =
                stockAlertSettingRepository.findByUserUserIdAndStockStockId(user.getUserId(), stockId)
                        .orElseThrow(() -> new StockAlertSettingNotFoundException()); // 예외처리

        // 엔티티 내부 메서드로 상태 변경 (유효성 검증 포함)
        // null이 아닌 필드만 업데이트 => updateDTO에서 int를 Integer로 변경
        if (request.getTargetPrice() != null) {
            setting.updateTargetPrice(request.getTargetPrice());
        }

        if (request.getCondition() != null) {
            setting.updateCondition(request.getCondition());
        }

        // 저장
        StockAlertSetting updatedSetting = stockAlertSettingRepository.save(setting);

        // 🔥 실시간 알람 조건 업데이트
        stockPriceProcessor.updateStockAlertCondition(updatedSetting);

        return StockAlertSettingResponseDTO.builder()
                .userId(updatedSetting.getUser().getUserId())
                .stockId(updatedSetting.getStock().getStockId())
                .targetPrice(updatedSetting.getTargetPrice())
                .condition(updatedSetting.getCondition())
                .build();
    }

    // 🔥 특정 유저의 특정 주식 설정 삭제 (수정됨)
    public StockAlertSettingResponseDTO deleteSetting(CustomUserDetails userDetails, String stockId) {

        if (userDetails == null || userDetails.getUserId() == null) {
            throw new AuthenticationRequiredException();
        }

        User user = userRepository.findById(userDetails.getUserId())
                .orElseThrow(()->new UserNotFoundException());

        Optional<StockAlertSetting> optSetting =
                stockAlertSettingRepository.findByUserUserIdAndStockStockId(user.getUserId(), stockId);

        if (optSetting.isEmpty()) {
            // 이미 설정이 없다면 아무것도 하지 않고 null 반환 (또는 Optional 반환해도 됨)
            return null;
        }

        StockAlertSetting setting = optSetting.get();

        StockAlertSettingResponseDTO deleteSetting =
                StockAlertSettingResponseDTO.builder()
                        .userId(setting.getUser().getUserId())
                        .stockId(setting.getStock().getStockId())
                        .targetPrice(setting.getTargetPrice())
                        .condition(setting.getCondition())
                        .build();

        // 🔥 실시간 알람 조건 제거 (삭제 전에 호출)
        stockPriceProcessor.removeCondition(
                setting.getSettingId(),
                setting.getStock().getStockId(),
                setting.getCondition(),
                setting.getTargetPrice()
        );

        stockAlertSettingRepository.delete(setting);

        return deleteSetting;
    }

}