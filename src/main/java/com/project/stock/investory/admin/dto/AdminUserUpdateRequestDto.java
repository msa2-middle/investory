package com.project.stock.investory.admin.dto;

import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
public class AdminUserUpdateRequestDto {
    private String name;
    private String phone;
    private String role;
}
