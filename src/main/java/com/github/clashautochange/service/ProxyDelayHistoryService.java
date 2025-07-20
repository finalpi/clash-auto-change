package com.github.clashautochange.service;

import com.github.clashautochange.entity.ProxyDelayHistory;
import com.github.clashautochange.repository.ProxyDelayHistoryRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 代理延迟历史记录服务
 */
@Service
public class ProxyDelayHistoryService {

    private final ProxyDelayHistoryRepository proxyDelayHistoryRepository;

    @Autowired
    public ProxyDelayHistoryService(ProxyDelayHistoryRepository proxyDelayHistoryRepository) {
        this.proxyDelayHistoryRepository = proxyDelayHistoryRepository;
    }

    /**
     * 保存代理延迟历史记录
     *
     * @param history 代理延迟历史记录
     * @return 保存后的记录
     */
    public ProxyDelayHistory saveHistory(ProxyDelayHistory history) {
        return proxyDelayHistoryRepository.save(history);
    }

    /**
     * 批量保存代理延迟历史记录
     *
     * @param histories 代理延迟历史记录列表
     * @return 保存后的记录列表
     */
    public List<ProxyDelayHistory> saveAllHistories(List<ProxyDelayHistory> histories) {
        return proxyDelayHistoryRepository.saveAll(histories);
    }

    /**
     * 获取指定代理组的所有历史记录
     *
     * @param groupName 代理组名称
     * @return 历史记录列表
     */
    public List<ProxyDelayHistory> getHistoriesByGroupName(String groupName) {
        return proxyDelayHistoryRepository.findByGroupNameOrderByTestTimeDesc(groupName);
    }

    /**
     * 获取指定代理组和代理节点的所有历史记录
     *
     * @param groupName 代理组名称
     * @param proxyName 代理节点名称
     * @return 历史记录列表
     */
    public List<ProxyDelayHistory> getHistoriesByGroupNameAndProxyName(String groupName, String proxyName) {
        return proxyDelayHistoryRepository.findByGroupNameAndProxyNameOrderByTestTimeDesc(groupName, proxyName);
    }

    /**
     * 获取指定时间范围内的历史记录
     *
     * @param groupName 代理组名称
     * @param startTime 开始时间
     * @param endTime 结束时间
     * @return 历史记录列表
     */
    public List<ProxyDelayHistory> getHistoriesByTimeRange(String groupName, LocalDateTime startTime, LocalDateTime endTime) {
        return proxyDelayHistoryRepository.findByGroupNameAndTestTimeBetweenOrderByTestTimeAsc(groupName, startTime, endTime);
    }

    /**
     * 获取指定代理组和代理节点在指定时间范围内的历史记录
     *
     * @param groupName 代理组名称
     * @param proxyName 代理节点名称
     * @param startTime 开始时间
     * @param endTime 结束时间
     * @return 历史记录列表
     */
    public List<ProxyDelayHistory> getHistoriesByProxyNameAndTimeRange(
            String groupName, String proxyName, LocalDateTime startTime, LocalDateTime endTime) {
        return proxyDelayHistoryRepository.findByGroupNameAndProxyNameAndTestTimeBetweenOrderByTestTimeDesc(
                groupName, proxyName, startTime, endTime);
    }

    /**
     * 获取最近7天的历史记录
     *
     * @param groupName 代理组名称
     * @return 历史记录列表
     */
    public List<ProxyDelayHistory> getLast7DaysHistories(String groupName) {
        LocalDateTime endTime = LocalDateTime.now();
        LocalDateTime startTime = endTime.minusDays(7);
        return getHistoriesByTimeRange(groupName, startTime, endTime);
    }

    /**
     * 获取指定代理组中的所有唯一代理节点名称
     *
     * @param groupName 代理组名称
     * @return 代理节点名称列表
     */
    public List<String> getDistinctProxyNamesByGroupName(String groupName) {
        return proxyDelayHistoryRepository.findDistinctProxyNamesByGroupName(groupName);
    }

    /**
     * 将延迟数据转换为图表格式
     *
     * @param histories 历史记录列表
     * @return 图表数据
     */
    public Map<String, Object> convertToChartData(List<ProxyDelayHistory> histories) {
        // 按代理节点名称分组
        Map<String, List<ProxyDelayHistory>> groupedByProxy = histories.stream()
                .collect(Collectors.groupingBy(ProxyDelayHistory::getProxyName));
        
        // 构建时间轴（X轴）
        List<String> timeLabels = histories.stream()
                .map(ProxyDelayHistory::getTestTime)
                .distinct()
                .sorted()
                .map(time -> time.toString())
                .collect(Collectors.toList());
        
        // 构建每个代理节点的延迟数据（Y轴）
        Map<String, List<Integer>> proxyDelays = groupedByProxy.entrySet().stream()
                .collect(Collectors.toMap(
                    Map.Entry::getKey,
                    entry -> entry.getValue().stream()
                            .sorted((a, b) -> a.getTestTime().compareTo(b.getTestTime()))
                            .map(ProxyDelayHistory::getDelay)
                            .collect(Collectors.toList())
                ));
        
        // 构建结果
        return Map.of(
            "timeLabels", timeLabels,
            "proxyDelays", proxyDelays
        );
    }

    /**
     * 定时任务：清理旧的历史记录（保留30天内的数据）
     */
    @Scheduled(cron = "0 0 0 * * ?") // 每天零点执行
    @Transactional
    public void cleanupOldHistories() {
        LocalDateTime thirtyDaysAgo = LocalDateTime.now().minusDays(30);
        long deletedCount = proxyDelayHistoryRepository.deleteByTestTimeBefore(thirtyDaysAgo);
    }

    public List<ProxyDelayHistory> findByGroupNameAndTimeRange(String groupName, LocalDateTime startTime, LocalDateTime endTime) {
        return proxyDelayHistoryRepository.findByGroupNameAndTestTimeBetween(groupName, startTime, endTime);
    }

    public List<ProxyDelayHistory> findByGroupNameAndProxyNameAndTimeRange(String groupName, String proxyName, LocalDateTime startTime, LocalDateTime endTime) {
        return proxyDelayHistoryRepository.findByGroupNameAndProxyNameAndTestTimeBetweenOrderByTestTimeDesc(groupName, proxyName, startTime, endTime);
    }
} 