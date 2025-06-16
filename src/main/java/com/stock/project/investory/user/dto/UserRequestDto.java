package com.stock.project.investory.user.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
public class UserRequestDto {

    @Email
    @NotBlank
    private String email;

    @Size(min = 6, message = "비밀번호는 최소 6자 이상이어야 합니다.")
    private String password;  // 소셜 로그인일 경우 null 허용

    @NotBlank
    private String name;

    @NotBlank
    private String phone;

    private Boolean isSocial = false;  // 기본값 false
}
