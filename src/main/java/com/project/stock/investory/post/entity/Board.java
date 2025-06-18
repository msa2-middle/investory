package com.project.stock.investory.post.entity;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "board")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Board {
    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "board_seq_generator")
    @SequenceGenerator(name = "board_seq_generator", sequenceName = "board_seq", allocationSize = 1)
    @Column(name = "board_id")
    private Long boardId;

    // 주식 관련 게시판이면 FK, VARCHAR(10)
    @Column(name = "stock_id", length = 10, unique = true)
    private String stockId;
}