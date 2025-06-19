package com.project.stock.investory.stockInfo.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class StockApiResponseDTO {

    //JSON의 key는 code, name

    @JsonProperty("code")
    private String stockId;

    @JsonProperty("name")
    private String stockName;
}
