package com.project.stock.investory.stockAlertSetting.service;


import com.project.stock.investory.stockAlertSetting.dto.StockAlertSettingCreateRequestDTO;
import com.project.stock.investory.stockAlertSetting.dto.StockAlertSettingResponseDTO;
import com.project.stock.investory.stockAlertSetting.dto.StockAlertSettingUpdateRequestDTO;
import com.project.stock.investory.stockAlertSetting.model.StockAlertSetting;
import com.project.stock.investory.stockAlertSetting.repository.StockAlertSettingRepository;
import com.project.stock.investory.stockInfo.model.Stock;
import com.project.stock.investory.stockInfo.repository.StockRepository;
import com.project.stock.investory.user.entity.User;
import com.project.stock.investory.user.repository.UserRepository;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class StockAlertSettingService {

    private final StockAlertSettingRepository stockAlertSettingRepository;
    private final UserRepository userRepository;
    private final StockRepository stockRepository;

    // 주가 알람 설정 생성
    public StockAlertSettingResponseDTO create(StockAlertSettingCreateRequestDTO request, Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new EntityNotFoundException()); // 예외처리

        Stock stock = stockRepository.findById(request.getStockId())
                .orElseThrow(() -> new EntityNotFoundException()); // 예외처리

        Optional<StockAlertSetting> existedSetting =
                stockAlertSettingRepository.findByUserUserIdAndStockStockId(userId, request.getStockId());

        // todo: 이걸로 중복 방지가 안되네요 해야할 일
        if (existedSetting.isPresent()) {
            throw new DuplicateKeyException("이미 해당 종목에 대한 알람이 설정되어 있습니다.");
        }

        StockAlertSetting stockAlertSetting =
                StockAlertSetting
                        .builder()
                        .user(user)
                        .stock(stock)
                        .targetPrice(request.getTargetPrice())
                        .condition(request.getCondition())
                        .build();

        StockAlertSetting savedStockAlertSetting = stockAlertSettingRepository.save(stockAlertSetting);

        return StockAlertSettingResponseDTO
                .builder()
                .userId(savedStockAlertSetting.getUser().getUserId())
                .stockId(savedStockAlertSetting.getStock().getStockId())
                .targetPrice(savedStockAlertSetting.getTargetPrice())
                .condition(savedStockAlertSetting.getCondition())
                .build();

    }


    // 특정 유저의 설정 전체 조회
    public List<StockAlertSettingResponseDTO> getUserSettings(Long userId) {

        User user = userRepository.findById(userId)
                .orElseThrow(()->new EntityNotFoundException());

        List<StockAlertSetting> settings = stockAlertSettingRepository.findByUserUserId(user.getUserId());

        if (settings.isEmpty()) {
            throw new EntityNotFoundException();
        }

        return settings.stream()
                .map(setting -> StockAlertSettingResponseDTO.builder()
                        .userId(setting.getUser().getUserId())
                        .stockId(setting.getStock().getStockId())
                        .targetPrice(setting.getTargetPrice())
                        .condition(setting.getCondition())
                        .build())
                .collect(Collectors.toList());
    }

    // 특정 유저의 특정 주식 설정 조회
    public StockAlertSettingResponseDTO getUserStockSettings(Long userId, String stockId) {

        StockAlertSetting setting =
                stockAlertSettingRepository.findByUserUserIdAndStockStockId(userId, stockId)
                .orElseThrow(() -> new EntityNotFoundException()); // 예외처리

        return StockAlertSettingResponseDTO.builder()
                .userId(setting.getUser().getUserId())
                .stockId(setting.getStock().getStockId())
                .targetPrice(setting.getTargetPrice())
                .condition(setting.getCondition())
                .build();
    }

    // 특정 유저의 특정 주식 설정 수정
    // 얘는 마이페이지로 갈 기능이니까 굳이 다른 사람이 수정하거나 할 기회가 없으니 예외처리할 필요가 있을까..
    public StockAlertSettingResponseDTO updateSetting(
            Long userId, String stockId, StockAlertSettingUpdateRequestDTO request
    ) {
        StockAlertSetting setting =
                stockAlertSettingRepository.findByUserUserIdAndStockStockId(userId, stockId)
                .orElseThrow(() -> new EntityNotFoundException()); // 예외처리

        // 엔티티 내부 메서드로 상태 변경 (유효성 검증 포함)
        setting.updateSetting(request.getTargetPrice(), request.getCondition());

        stockAlertSettingRepository.save(setting);

        return StockAlertSettingResponseDTO.builder()
                .userId(setting.getUser().getUserId())
                .stockId(setting.getStock().getStockId())
                .targetPrice(setting.getTargetPrice())
                .condition(setting.getCondition())
                .build();
    }

    // 특정 유저의 특정 주식 설정 삭제
    public StockAlertSettingResponseDTO deleteSetting(Long userId, String stockId) {

        StockAlertSetting setting =
                stockAlertSettingRepository.findByUserUserIdAndStockStockId(userId, stockId)
                .orElseThrow(() -> new EntityNotFoundException()); // 예외처리

        StockAlertSettingResponseDTO deleteSetting =
                StockAlertSettingResponseDTO.builder()
                        .userId(setting.getUser().getUserId())
                        .stockId(setting.getStock().getStockId())
                        .targetPrice(setting.getTargetPrice())
                        .condition(setting.getCondition())
                        .build();

        stockAlertSettingRepository.delete(setting);

        return deleteSetting;
    }

}
