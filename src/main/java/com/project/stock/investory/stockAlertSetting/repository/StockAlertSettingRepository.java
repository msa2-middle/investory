package com.project.stock.investory.stockAlertSetting.repository;

import com.project.stock.investory.stockAlertSetting.model.StockAlertSetting;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface StockAlertSettingRepository extends JpaRepository<StockAlertSetting, Long> {

    List<StockAlertSetting> findByUserUserId(Long userId);

    Optional<StockAlertSetting> findByUserUserIdAndStockStockId(Long userId, String stockId);

    List<StockAlertSetting> findByIsActiveTrue();
}


