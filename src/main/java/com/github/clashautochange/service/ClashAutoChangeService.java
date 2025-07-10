package com.github.clashautochange.service;

import com.github.clashautochange.entity.ProxyGroupConfig;
import com.github.clashautochange.model.ClashDelayResponse;
import com.github.clashautochange.model.ClashProxy;
import com.github.clashautochange.model.ClashProxiesResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;

/**
 * Clash 自动切换服务
 * 定期检查代理延迟并自动切换到最佳代理
 */
@Service
@EnableScheduling
@Slf4j
public class ClashAutoChangeService {

    private final ClashApiService clashApiService;
    private final ProxyGroupConfigService proxyGroupConfigService;

    @Value("${clash.auto-change.test-url:https://www.gstatic.com/generate_204}")
    private String testUrl;

    @Autowired
    public ClashAutoChangeService(ClashApiService clashApiService, ProxyGroupConfigService proxyGroupConfigService) {
        this.clashApiService = clashApiService;
        this.proxyGroupConfigService = proxyGroupConfigService;
    }

    /**
     * 定时任务，自动切换代理
     * 根据数据库中的配置，检查每个策略组的代理延迟并进行切换
     */
    @Scheduled(fixedDelayString = "${clash.auto-change.check-interval:5000}")
    public void autoChangeProxy() {
        // 获取所有已启用的策略组配置
        List<ProxyGroupConfig> enabledConfigs = proxyGroupConfigService.getAllEnabledConfigs();
        if (enabledConfigs.isEmpty()) {
            return;
        }

        // 处理每个已启用的策略组配置
        for (ProxyGroupConfig config : enabledConfigs) {
            processProxyGroup(config);
        }
    }

    /**
     * 处理单个策略组
     *
     * @param config 策略组配置
     */
    private void processProxyGroup(ProxyGroupConfig config) {
        String groupName = config.getGroupName();
        String preferredProxy = config.getPreferredProxy();
        Integer timeout = config.getTimeout();
        Integer maxDelay = config.getMaxDelay();
        Integer maxTimeoutCount = config.getMaxTimeoutCount();
        String testUrlToUse = config.getTestUrl() != null ? config.getTestUrl() : this.testUrl;

        try {
            // 获取策略组信息
            Map<String, Object> groupInfo = clashApiService.getGroupInfo(groupName);
            if (groupInfo.isEmpty()) {
                return;
            }

            // 获取当前选中的代理
            String currentProxy = (String) groupInfo.get("now");
            if (currentProxy == null) {
                return;
            }

            // 一次性测试所有节点的延迟
            Map<String, Integer> delayResults = clashApiService.testGroupDelay(groupName, testUrlToUse, timeout);

            if (delayResults.isEmpty()) {
                return;
            }

            // 检查优先节点是否可用
            Integer preferredDelay = delayResults.get(preferredProxy);
            if (preferredDelay != null && preferredDelay <= maxDelay) {
                // 优先节点可用且延迟在可接受范围内，直接切换，忽略超时次数
                if (!preferredProxy.equals(currentProxy)) {
                    log.info("优先节点可用，直接切换: {} -> {}, 延迟: {}ms", currentProxy, preferredProxy, preferredDelay);
                    clashApiService.selectProxy(groupName, preferredProxy);
                }
                // 重置超时计数
                if (config.getCurrentTimeoutCount() > 0) {
                    config.setCurrentTimeoutCount(0);
                    proxyGroupConfigService.saveConfig(config);
                }
                return;
            }

            // 检查当前节点是否可用
            Integer currentDelay = delayResults.get(currentProxy);
            boolean isCurrentProxyAvailable = currentDelay != null && currentDelay <= maxDelay;

            if (isCurrentProxyAvailable) {
                // 当前节点可用，重置超时计数并保持不变
                if (config.getCurrentTimeoutCount() > 0) {
                    config.setCurrentTimeoutCount(0);
                    proxyGroupConfigService.saveConfig(config);
                }
                // 保持当前节点稳定，不寻找更低延迟的节点
                return;
            } else {
                // 当前节点不可用，增加超时计数
                config.setCurrentTimeoutCount(config.getCurrentTimeoutCount() + 1);
                proxyGroupConfigService.saveConfig(config);
                
                // 检查是否超过最大超时次数
                if (config.getCurrentTimeoutCount() < maxTimeoutCount) {
                    log.info("节点 {} 超时，当前超时计数: {}/{}", currentProxy, config.getCurrentTimeoutCount(), maxTimeoutCount);
                    return; // 未达到最大超时次数，不切换
                }
                
                log.info("节点 {} 连续超时 {} 次，开始寻找可用节点", currentProxy, config.getCurrentTimeoutCount());
            }

            // 找出延迟最低且小于最大延迟的代理
            Optional<Map.Entry<String, Integer>> bestProxy = delayResults.entrySet().stream()
                    .filter(entry -> entry.getValue() <= maxDelay)
                    .min(Map.Entry.comparingByValue());

            if (bestProxy.isPresent()) {
                String bestProxyName = bestProxy.get().getKey();
                Integer bestDelay = bestProxy.get().getValue();

                // 切换到最佳代理
                log.info("切换代理: {} -> {}, 延迟: {}ms", currentProxy, bestProxyName, bestDelay);
                clashApiService.selectProxy(groupName, bestProxyName);
                
                // 切换后重置超时计数
                config.setCurrentTimeoutCount(0);
                proxyGroupConfigService.saveConfig(config);
            }
        } catch (Exception e) {
            log.error("处理策略组 {} 时出错: {}", groupName, e.getMessage());
        }
    }
}