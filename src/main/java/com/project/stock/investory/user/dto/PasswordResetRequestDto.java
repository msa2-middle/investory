package com.project.stock.investory.user.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
public class PasswordResetRequestDto {

    @Email
    @NotBlank
    private String email;

    @Size(min = 6, message = "비밀번호는 최소 6자 이상이어야 합니다.")
    @NotBlank
    private String newPassword;
}
