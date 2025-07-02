package com.project.stock.investory.user.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
public class PasswordUpdateRequestDto {

    @NotBlank
    private String currentPassword;  // 현재 비밀번호

    @NotBlank
    private String newPassword;      // 변경할 새 비밀번호
}
