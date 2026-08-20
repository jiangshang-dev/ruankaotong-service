package com.heima.config;

import com.heima.web.AdminAuthInterceptor;
import com.heima.web.ClientAuthInterceptor;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class WebConfig implements WebMvcConfigurer {

    private final AdminAuthInterceptor adminAuthInterceptor;
    private final ClientAuthInterceptor clientAuthInterceptor;

    public WebConfig(AdminAuthInterceptor adminAuthInterceptor, ClientAuthInterceptor clientAuthInterceptor) {
        this.adminAuthInterceptor = adminAuthInterceptor;
        this.clientAuthInterceptor = clientAuthInterceptor;
    }

    @Override
    public void addCorsMappings(CorsRegistry registry) {
        registry.addMapping("/api/**")
                .allowedOriginPatterns("*")
                .allowedMethods("GET", "POST", "PUT", "DELETE", "OPTIONS")
                .allowedHeaders("*");
    }

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(adminAuthInterceptor)
                .addPathPatterns("/api/admin/**")
                .excludePathPatterns("/api/admin/login");
        registry.addInterceptor(clientAuthInterceptor)
                .addPathPatterns("/api/ai/**")
                .excludePathPatterns(
                        "/api/ai/essay/health",
                        "/api/ai/case/health",
                        "/api/ai/knowledge/health");
    }
}
