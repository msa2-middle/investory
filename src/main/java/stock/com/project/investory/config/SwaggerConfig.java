package stock.com.project.investory.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class SwaggerConfig {
    @Bean
    public OpenAPI openAPI() {  //TODO: JWT 사용 시 설정 바꿔야함
        return new OpenAPI()
                .components(new Components())
                .info(apiInfo());
    }

    private Info apiInfo() {
        return new Info()
                .title("Investory")  // API 탙이틀
                .description("주식 정보 및 커뮤니티 API") //API 상세소개 및 사용법 등
                .version("1.0.0");
    }
}
