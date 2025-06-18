package com.project.stock.investory.stockInfo.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class StockApiResponseDTO {
    private String stockId;
    private String stockName;
}
