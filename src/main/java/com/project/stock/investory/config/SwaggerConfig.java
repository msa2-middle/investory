package com.project.stock.investory.config;

import io.swagger.v3.oas.annotations.enums.SecuritySchemeType;
import io.swagger.v3.oas.annotations.security.SecurityScheme;
import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Swagger 설정 클래스
 * - JWT 인증을 위한 Authorize 버튼 활성화
 */
@Configuration
@SecurityScheme(
        name = "BearerAuth",               // 이 이름으로 보안 스키마를 등록하고 참조함
        type = SecuritySchemeType.HTTP,   // HTTP 인증 방식 사용
        scheme = "bearer",                // bearer 스키마 (Authorization: Bearer <token>)
        bearerFormat = "JWT"              // 형식은 JWT
)
public class SwaggerConfig {

    @Bean
    public OpenAPI openAPI() {
        return new OpenAPI()
                .info(apiInfo()) // API 메타 정보 설정
                // 전역 보안 설정 (모든 API에 아래 보안 스키마 적용)
                .addSecurityItem(new SecurityRequirement().addList("BearerAuth"))
                .components(new Components()
                        // 실제 보안 스키마 정의 (import 충돌 피하기 위해 전체 경로 명시)
                        .addSecuritySchemes("BearerAuth",
                                new io.swagger.v3.oas.models.security.SecurityScheme()
                                        .name("BearerAuth")
                                        .type(io.swagger.v3.oas.models.security.SecurityScheme.Type.HTTP)
                                        .scheme("bearer")
                                        .bearerFormat("JWT")));
    }

    // Swagger 화면에 표시될 API 기본 정보 설정
    private Info apiInfo() {
        return new Info()
                .title("Investory")
                .description("주식 정보 및 커뮤니티 API")
                .version("1.0.0");
    }
}
