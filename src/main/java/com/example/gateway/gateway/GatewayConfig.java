package com.example.gateway.gateway;

import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class GatewayConfig {

    @Bean
    public FilterRegistrationBean<GatewayFilter> gatewayFilterRegistration(GatewayFilter filter) {
        FilterRegistrationBean<GatewayFilter> reg = new FilterRegistrationBean<>(filter);
        reg.addUrlPatterns("/leave/*", "/attendance/*");
        return reg;
    }
}
