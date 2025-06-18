package com.project.stock.investory.stockInfo.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import lombok.*;

@Entity
@NoArgsConstructor
@AllArgsConstructor
@Getter
@Setter
@Builder
public class Stock {
    @Id
    @Column(name = "stock_id", length = 10, unique = true)
    private String stockId;

    @Column(name = "stock_name")
    private String stockName;
}
