package com.project.stock.investory.user.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
public class VerifyCodeRequestDto {

    @Email
    @NotBlank
    private String email;

    @NotBlank
    private String code;
}
