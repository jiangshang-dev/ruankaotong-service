package com.heima.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI ruankaoOpenAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("软考通 AI API")
                        .description("论文润色/评分、案例分析识图解答/评分（AgentScope 2，无库无会话记忆）")
                        .version("1.0.0")
                        .contact(new Contact().name("ruankao-back")));
    }
}
