package com.github.clashautochange.config;

import com.github.clashautochange.service.SystemConfigService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Lazy;
import org.springframework.web.client.RestTemplate;

@Configuration
public class ClashApiConfig {

    @Value("${clash.api.base-url}")
    private String defaultBaseUrl;

    @Value("${clash.api.secret:}")
    private String defaultSecret;
    
    private final SystemConfigService systemConfigService;
    
    @Autowired
    public ClashApiConfig(@Lazy SystemConfigService systemConfigService) {
        this.systemConfigService = systemConfigService;
    }

    @Bean
    public RestTemplate restTemplate() {
        return new RestTemplate();
    }

    public String getBaseUrl() {
        try {
            String baseUrl = systemConfigService.getConfigValue("clash.api.base-url");
            return baseUrl.isEmpty() ? defaultBaseUrl : baseUrl;
        } catch (Exception e) {
            return defaultBaseUrl;
        }
    }

    public String getSecret() {
        try {
            String secret = systemConfigService.getConfigValue("clash.api.secret");
            return secret.isEmpty() ? defaultSecret : secret;
        } catch (Exception e) {
            return defaultSecret;
        }
    }
} 