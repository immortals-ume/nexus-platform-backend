package com.immortals.config.server.config;

import com.immortals.config.server.observability.ConfigAccessInterceptor;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
@RequiredArgsConstructor
public class WebMvcConfig implements WebMvcConfigurer {

    private final ConfigAccessInterceptor configAccessInterceptor;

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(configAccessInterceptor)
            .addPathPatterns("/**")
            .excludePathPatterns("/actuator/health", "/actuator/info");
    }
}
