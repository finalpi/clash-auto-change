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

    @Value("${clash.auto-change.timeout:5000}")
    private Integer timeout;

    @Value("${clash.auto-change.proxy-group:}")
    private String proxyGroup;

    @Value("${clash.auto-change.max-delay:500}")
    private Integer maxDelay;

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
            log.info("没有启用的策略组配置");
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
        String testUrlToUse = config.getTestUrl() != null ? config.getTestUrl() : this.testUrl;

        log.info("处理策略组: {}, 优先节点: {}, 最大延迟: {}ms, 测试URL: {}", 
                groupName, preferredProxy, maxDelay, testUrlToUse);

        try {
            // 获取策略组信息
            Map<String, Object> groupInfo = clashApiService.getGroupInfo(groupName);
            if (groupInfo.isEmpty()) {
                log.error("策略组 {} 不存在或无法获取信息", groupName);
                return;
            }
            
            // 获取当前选中的代理
            String currentProxy = (String) groupInfo.get("now");
            if (currentProxy == null) {
                log.error("无法获取策略组 {} 的当前选择", groupName);
                return;
            }
            log.info("当前选中的代理: {}", currentProxy);

            // 一次性测试所有节点的延迟
            log.info("开始测试策略组 {} 中所有节点的延迟", groupName);
            Map<String, Integer> delayResults = clashApiService.testGroupDelay(groupName, testUrlToUse, timeout);
            
            if (delayResults.isEmpty()) {
                log.warn("测试结果为空，所有节点可能都超时");
                return;
            }
            
            log.info("获取到 {} 个节点的延迟结果: {}", delayResults.size(), delayResults);

            // 检查优先节点是否可用
            Integer preferredDelay = delayResults.get(preferredProxy);
            if (preferredDelay != null && preferredDelay <= maxDelay) {
                // 优先节点可用且延迟在可接受范围内
                if (!preferredProxy.equals(currentProxy)) {
                    log.info("优先节点 {} 可用，延迟: {}ms，切换到优先节点", preferredProxy, preferredDelay);
                    clashApiService.selectProxy(groupName, preferredProxy);
                } else {
                    log.info("当前已是优先节点 {} 且可用，延迟: {}ms，无需切换", preferredProxy, preferredDelay);
                }
                return;
            }
            
            // 检查当前节点是否可用
            Integer currentDelay = delayResults.get(currentProxy);
            boolean isCurrentProxyAvailable = currentDelay != null && currentDelay <= maxDelay;
            
            // 如果优先节点不可用或延迟过高，查找延迟最低的节点
            log.info("优先节点 {} 不可用或延迟过高: {}", preferredProxy, 
                    preferredDelay != null ? preferredDelay + "ms" : "无响应");
            
            // 如果当前节点可用，且优先节点不可用，则不切换
            if (isCurrentProxyAvailable && (preferredDelay == null || preferredDelay > maxDelay)) {
                log.info("当前节点 {} 可用(延迟: {}ms)，优先节点不可用，保持当前节点", currentProxy, currentDelay);
                return;
            }

            // 找出延迟最低且小于最大延迟的代理
            Optional<Map.Entry<String, Integer>> bestProxy = delayResults.entrySet().stream()
                    .filter(entry -> entry.getValue() <= maxDelay)
                    .min(Map.Entry.comparingByValue());

            if (bestProxy.isPresent()) {
                String bestProxyName = bestProxy.get().getKey();
                Integer bestDelay = bestProxy.get().getValue();

                // 只有当最佳代理不是当前代理时才切换
                if (!bestProxyName.equals(currentProxy)) {
                    log.info("切换到延迟最低的代理 {}, 延迟: {}ms", bestProxyName, bestDelay);
                    clashApiService.selectProxy(groupName, bestProxyName);
                } else {
                    log.info("当前代理 {} 已是延迟最低的代理, 延迟: {}ms", currentProxy, bestDelay);
                }
            } else {
                log.warn("没有找到延迟小于 {}ms 的代理", maxDelay);
            }
        } catch (Exception e) {
            log.error("处理策略组 {} 时出错: {}", groupName, e.getMessage(), e);
        }
    }
}