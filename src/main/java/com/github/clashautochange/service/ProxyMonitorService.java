package com.github.clashautochange.service;

import com.github.clashautochange.entity.MonitoredProxyGroup;
import com.github.clashautochange.entity.ProxyDelayHistory;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * 代理监控服务
 * 定时监控代理组节点的延迟
 */
@Service
@Slf4j
public class ProxyMonitorService {

    private final ClashApiService clashApiService;
    private final MonitoredProxyGroupService monitoredProxyGroupService;
    private final ProxyDelayHistoryService proxyDelayHistoryService;

    @Autowired
    public ProxyMonitorService(
            ClashApiService clashApiService,
            MonitoredProxyGroupService monitoredProxyGroupService,
            ProxyDelayHistoryService proxyDelayHistoryService) {
        this.clashApiService = clashApiService;
        this.monitoredProxyGroupService = monitoredProxyGroupService;
        this.proxyDelayHistoryService = proxyDelayHistoryService;
    }

    /**
     * 定时任务，每分钟监控一次代理组节点延迟
     */
    @Scheduled(fixedRate = 10000) // 每10秒执行一次
    public void monitorProxyDelay() {
        // 获取所有启用的监控代理组
        List<MonitoredProxyGroup> enabledGroups = monitoredProxyGroupService.getAllEnabledConfigs();
        if (enabledGroups.isEmpty()) {
            return;
        }

        log.info("开始监控代理组延迟，共 {} 个代理组", enabledGroups.size());

        // 处理每个代理组
        for (MonitoredProxyGroup group : enabledGroups) {
            try {
                processProxyGroup(group);
            } catch (Exception e) {
                log.error("监控代理组 {} 时出错: {}", group.getGroupName(), e.getMessage());
            }
        }
    }

    /**
     * 处理单个代理组
     *
     * @param group 监控代理组配置
     */
    private void processProxyGroup(MonitoredProxyGroup group) {
        String groupName = group.getGroupName();
        String testUrl = group.getTestUrl();
        Integer timeout = group.getTimeout();

        log.info("监控代理组: {}, 测试URL: {}, 超时: {}ms", groupName, testUrl, timeout);

        try {
            // 获取代理组中的所有节点
            List<String> proxies = clashApiService.getGroupProxies(groupName);
            if (proxies.isEmpty()) {
                log.warn("代理组 {} 中没有找到代理节点", groupName);
                return;
            }

            log.info("代理组 {} 中找到 {} 个代理节点", groupName, proxies.size());

            // 测试所有节点的延迟
            Map<String, Integer> delayResults = clashApiService.testGroupDelay(groupName, testUrl, timeout);
            if (delayResults.isEmpty()) {
                log.warn("代理组 {} 延迟测试结果为空", groupName);
                return;
            }

            // 保存延迟历史记录
            List<ProxyDelayHistory> histories = new ArrayList<>();
            for (String proxy : proxies) {
                Integer delay = delayResults.getOrDefault(proxy, -1); // 默认为-1表示未连通
                ProxyDelayHistory history = ProxyDelayHistory.create(groupName, proxy, delay);
                histories.add(history);

                log.debug("代理节点: {}, 延迟: {}ms", proxy, delay);
            }

            proxyDelayHistoryService.saveAllHistories(histories);
            log.info("成功保存代理组 {} 的 {} 条延迟历史记录", groupName, histories.size());

        } catch (Exception e) {
            log.error("处理代理组 {} 时出错: {}", groupName, e.getMessage());
        }
    }
}