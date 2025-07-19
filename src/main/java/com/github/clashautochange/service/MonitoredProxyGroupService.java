package com.github.clashautochange.service;

import com.github.clashautochange.entity.MonitoredProxyGroup;
import com.github.clashautochange.repository.MonitoredProxyGroupRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

/**
 * 监控代理组服务
 */
@Service
public class MonitoredProxyGroupService {

    private final MonitoredProxyGroupRepository monitoredProxyGroupRepository;

    @Autowired
    public MonitoredProxyGroupService(MonitoredProxyGroupRepository monitoredProxyGroupRepository) {
        this.monitoredProxyGroupRepository = monitoredProxyGroupRepository;
    }

    /**
     * 保存监控代理组配置
     *
     * @param config 监控代理组配置
     * @return 保存后的配置
     */
    public MonitoredProxyGroup saveConfig(MonitoredProxyGroup config) {
        return monitoredProxyGroupRepository.save(config);
    }

    /**
     * 获取所有监控代理组配置
     *
     * @return 所有监控代理组配置
     */
    public List<MonitoredProxyGroup> getAllConfigs() {
        return monitoredProxyGroupRepository.findAll();
    }

    /**
     * 获取所有已启用的监控代理组配置
     *
     * @return 所有已启用的监控代理组配置
     */
    public List<MonitoredProxyGroup> getAllEnabledConfigs() {
        return monitoredProxyGroupRepository.findByEnabledTrue();
    }

    /**
     * 根据ID获取监控代理组配置
     *
     * @param id 配置ID
     * @return 监控代理组配置
     */
    public Optional<MonitoredProxyGroup> getConfigById(Long id) {
        return monitoredProxyGroupRepository.findById(id);
    }

    /**
     * 根据代理组名称获取配置
     *
     * @param groupName 代理组名称
     * @return 监控代理组配置
     */
    public Optional<MonitoredProxyGroup> getConfigByGroupName(String groupName) {
        return monitoredProxyGroupRepository.findByGroupName(groupName);
    }

    /**
     * 删除监控代理组配置
     *
     * @param id 配置ID
     */
    public void deleteConfig(Long id) {
        monitoredProxyGroupRepository.deleteById(id);
    }

    /**
     * 切换监控代理组配置的启用状态
     *
     * @param id 配置ID
     * @return 更新后的配置，如果不存在则返回空
     */
    public Optional<MonitoredProxyGroup> toggleEnabled(Long id) {
        Optional<MonitoredProxyGroup> configOpt = monitoredProxyGroupRepository.findById(id);
        if (configOpt.isPresent()) {
            MonitoredProxyGroup config = configOpt.get();
            config.setEnabled(!config.getEnabled());
            return Optional.of(monitoredProxyGroupRepository.save(config));
        }
        return Optional.empty();
    }
} 