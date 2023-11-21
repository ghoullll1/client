package com.example.client.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.filter.CorsFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;
import org.springframework.context.annotation.Bean;

@Configuration
public class CorsConfig {

    @Bean
    public CorsFilter corsFilter() {
        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        CorsConfiguration config = new CorsConfiguration();
        config.addAllowedOrigin("*"); // 允许所有来源访问，生产环境中应该更精确地配置允许的来源
        config.addAllowedMethod("*"); // 允许所有请求方法
        config.addAllowedHeader("*"); // 允许所有请求头
        source.registerCorsConfiguration("/**", config);
        return new CorsFilter(source);
    }
}