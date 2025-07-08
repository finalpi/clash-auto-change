package com.github.clashautochange.service;

import com.github.clashautochange.entity.SystemConfig;
import com.github.clashautochange.repository.SystemConfigRepository;
import jakarta.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * 系统配置服务
 */
@Service
public class SystemConfigService {

    private final SystemConfigRepository systemConfigRepository;

    @Value("${clash.api.base-url}")
    private String defaultBaseUrl;

    @Value("${clash.api.secret}")
    private String defaultSecret;

    @Autowired
    public SystemConfigService(SystemConfigRepository systemConfigRepository) {
        this.systemConfigRepository = systemConfigRepository;
    }

    /**
     * 初始化系统配置
     */
    @PostConstruct
    @Transactional
    public void init() {
        try {
            // 初始化Clash API配置
            initConfig("clash.api.base-url", defaultBaseUrl, "Clash API基础URL");
            initConfig("clash.api.secret", defaultSecret, "Clash API密钥");
        } catch (Exception e) {
            // 记录错误但不中断应用启动
            System.err.println("初始化系统配置时出错: " + e.getMessage());
            e.printStackTrace();
        }
    }

    /**
     * 初始化配置项
     * 
     * @param key 配置键
     * @param defaultValue 默认值
     * @param description 描述
     */
    @Transactional
    private void initConfig(String key, String defaultValue, String description) {
        Optional<SystemConfig> existingConfig = systemConfigRepository.findByConfigKey(key);
        if (existingConfig.isEmpty()) {
            SystemConfig config = new SystemConfig();
            config.setConfigKey(key);
            config.setConfigValue(defaultValue != null ? defaultValue : "");
            config.setDescription(description);
            systemConfigRepository.save(config);
        }
    }

    /**
     * 获取所有系统配置
     * 
     * @return 所有系统配置
     */
    public List<SystemConfig> getAllConfigs() {
        return systemConfigRepository.findAll();
    }

    /**
     * 获取配置值
     * 
     * @param key 配置键
     * @return 配置值
     */
    public String getConfigValue(String key) {
        Optional<SystemConfig> config = systemConfigRepository.findByConfigKey(key);
        return config.map(SystemConfig::getConfigValue).orElse("");
    }

    /**
     * 获取Clash API配置
     * 
     * @return Clash API配置
     */
    public Map<String, String> getClashApiConfig() {
        Map<String, String> config = new HashMap<>();
        
        String baseUrl = getConfigValue("clash.api.base-url");
        if (baseUrl.isEmpty()) {
            baseUrl = defaultBaseUrl;
        }
        config.put("baseUrl", baseUrl);
        
        String secret = getConfigValue("clash.api.secret");
        if (secret.isEmpty()) {
            secret = defaultSecret;
        }
        config.put("secret", secret);
        
        return config;
    }

    /**
     * 保存系统配置
     * 
     * @param config 系统配置
     * @return 保存后的配置
     */
    @Transactional
    public SystemConfig saveConfig(SystemConfig config) {
        return systemConfigRepository.save(config);
    }

    /**
     * 更新Clash API配置
     * 
     * @param baseUrl Clash API基础URL
     * @param secret Clash API密钥
     */
    @Transactional
    public void updateClashApiConfig(String baseUrl, String secret) {
        SystemConfig baseUrlConfig = systemConfigRepository.findByConfigKey("clash.api.base-url")
                .orElse(new SystemConfig("clash.api.base-url", baseUrl, "Clash API基础URL"));
        baseUrlConfig.setConfigValue(baseUrl);
        systemConfigRepository.save(baseUrlConfig);

        SystemConfig secretConfig = systemConfigRepository.findByConfigKey("clash.api.secret")
                .orElse(new SystemConfig("clash.api.secret", secret, "Clash API密钥"));
        secretConfig.setConfigValue(secret);
        systemConfigRepository.save(secretConfig);
    }
} 