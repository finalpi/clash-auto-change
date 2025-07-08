package com.github.clashautochange.service;

import com.github.clashautochange.entity.ProxyGroupConfig;
import com.github.clashautochange.repository.ProxyGroupConfigRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

/**
 * 策略组配置服务
 */
@Service
public class ProxyGroupConfigService {

    private final ProxyGroupConfigRepository proxyGroupConfigRepository;

    @Autowired
    public ProxyGroupConfigService(ProxyGroupConfigRepository proxyGroupConfigRepository) {
        this.proxyGroupConfigRepository = proxyGroupConfigRepository;
    }

    /**
     * 保存策略组配置
     *
     * @param config 策略组配置
     * @return 保存后的配置
     */
    public ProxyGroupConfig saveConfig(ProxyGroupConfig config) {
        return proxyGroupConfigRepository.save(config);
    }

    /**
     * 获取所有策略组配置
     *
     * @return 所有策略组配置
     */
    public List<ProxyGroupConfig> getAllConfigs() {
        return proxyGroupConfigRepository.findAll();
    }

    /**
     * 获取所有已启用的策略组配置
     *
     * @return 所有已启用的策略组配置
     */
    public List<ProxyGroupConfig> getAllEnabledConfigs() {
        return proxyGroupConfigRepository.findByEnabledTrue();
    }

    /**
     * 根据ID获取策略组配置
     *
     * @param id 配置ID
     * @return 策略组配置
     */
    public Optional<ProxyGroupConfig> getConfigById(Long id) {
        return proxyGroupConfigRepository.findById(id);
    }

    /**
     * 根据策略组名称获取配置
     *
     * @param groupName 策略组名称
     * @return 策略组配置
     */
    public Optional<ProxyGroupConfig> getConfigByGroupName(String groupName) {
        return proxyGroupConfigRepository.findByGroupName(groupName);
    }

    /**
     * 删除策略组配置
     *
     * @param id 配置ID
     */
    public void deleteConfig(Long id) {
        proxyGroupConfigRepository.deleteById(id);
    }
} 