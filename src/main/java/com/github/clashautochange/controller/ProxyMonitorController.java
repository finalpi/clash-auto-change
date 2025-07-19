package com.github.clashautochange.controller;

import com.github.clashautochange.entity.MonitoredProxyGroup;
import com.github.clashautochange.entity.ProxyDelayHistory;
import com.github.clashautochange.service.ClashApiService;
import com.github.clashautochange.service.MonitoredProxyGroupService;
import com.github.clashautochange.service.ProxyDelayHistoryService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * 代理监控控制器
 */
@Controller
@RequestMapping("/proxy-monitor")
public class ProxyMonitorController {

    private final MonitoredProxyGroupService monitoredProxyGroupService;
    private final ProxyDelayHistoryService proxyDelayHistoryService;
    private final ClashApiService clashApiService;

    @Autowired
    public ProxyMonitorController(
            MonitoredProxyGroupService monitoredProxyGroupService,
            ProxyDelayHistoryService proxyDelayHistoryService,
            ClashApiService clashApiService) {
        this.monitoredProxyGroupService = monitoredProxyGroupService;
        this.proxyDelayHistoryService = proxyDelayHistoryService;
        this.clashApiService = clashApiService;
    }

    /**
     * 显示代理监控页面
     */
    @GetMapping
    public String showMonitorPage(Model model) {
        List<MonitoredProxyGroup> configs = monitoredProxyGroupService.getAllConfigs();
        model.addAttribute("configs", configs);
        model.addAttribute("newConfig", new MonitoredProxyGroup());
        
        // 获取可用的代理组
        Map<String, List<String>> availableGroups = clashApiService.getAllGroups();
        model.addAttribute("availableGroups", availableGroups);
        
        return "proxy-monitor";
    }

    /**
     * 添加监控代理组配置
     */
    @PostMapping("/add")
    public String addMonitorConfig(
            @ModelAttribute MonitoredProxyGroup config,
            RedirectAttributes redirectAttributes) {
        try {
            // 设置默认值
            config.setEnabled(true);
            monitoredProxyGroupService.saveConfig(config);
            redirectAttributes.addFlashAttribute("message", "监控配置添加成功");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", "添加监控配置失败: " + e.getMessage());
        }
        return "redirect:/proxy-monitor";
    }

    /**
     * 更新监控代理组配置
     */
    @PostMapping("/update/{id}")
    public String updateMonitorConfig(
            @PathVariable Long id,
            @ModelAttribute MonitoredProxyGroup config,
            RedirectAttributes redirectAttributes) {
        try {
            Optional<MonitoredProxyGroup> existingConfig = monitoredProxyGroupService.getConfigById(id);
            if (existingConfig.isPresent()) {
                config.setId(id);
                monitoredProxyGroupService.saveConfig(config);
                redirectAttributes.addFlashAttribute("message", "监控配置更新成功");
            } else {
                redirectAttributes.addFlashAttribute("error", "未找到ID为 " + id + " 的监控配置");
            }
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", "更新监控配置失败: " + e.getMessage());
        }
        return "redirect:/proxy-monitor";
    }

    /**
     * 删除监控代理组配置
     */
    @GetMapping("/delete/{id}")
    public String deleteMonitorConfig(
            @PathVariable Long id,
            RedirectAttributes redirectAttributes) {
        try {
            monitoredProxyGroupService.deleteConfig(id);
            redirectAttributes.addFlashAttribute("message", "监控配置删除成功");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", "删除监控配置失败: " + e.getMessage());
        }
        return "redirect:/proxy-monitor";
    }

    /**
     * 切换监控代理组配置的启用状态
     */
    @GetMapping("/toggle/{id}")
    public String toggleMonitorConfig(
            @PathVariable Long id,
            RedirectAttributes redirectAttributes) {
        try {
            Optional<MonitoredProxyGroup> updatedConfig = monitoredProxyGroupService.toggleEnabled(id);
            if (updatedConfig.isPresent()) {
                String status = updatedConfig.get().getEnabled() ? "启用" : "禁用";
                redirectAttributes.addFlashAttribute("message", "监控配置已" + status);
            } else {
                redirectAttributes.addFlashAttribute("error", "未找到ID为 " + id + " 的监控配置");
            }
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", "切换监控配置状态失败: " + e.getMessage());
        }
        return "redirect:/proxy-monitor";
    }

    /**
     * 显示代理组延迟历史记录
     */
    @GetMapping("/history/{groupName}")
    public String showDelayHistory(
            @PathVariable String groupName,
            Model model) {
        Optional<MonitoredProxyGroup> config = monitoredProxyGroupService.getConfigByGroupName(groupName);
        if (config.isPresent()) {
            model.addAttribute("config", config.get());
            
            // 获取最近7天的历史记录
            List<ProxyDelayHistory> histories = proxyDelayHistoryService.getLast7DaysHistories(groupName);
            model.addAttribute("histories", histories);
            
            // 获取代理组中的所有代理节点
            List<String> proxyNames = proxyDelayHistoryService.getDistinctProxyNamesByGroupName(groupName);
            model.addAttribute("proxyNames", proxyNames);
            
            return "proxy-history";
        } else {
            return "redirect:/proxy-monitor";
        }
    }

    /**
     * 获取代理组延迟历史数据（用于图表）
     */
    @GetMapping("/api/history-data/{groupName}")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> getDelayHistoryData(
            @PathVariable String groupName,
            @RequestParam(required = false) String proxyName,
            @RequestParam(required = false) Integer days) {
        
        LocalDateTime endTime = LocalDateTime.now();
        LocalDateTime startTime = endTime.minusDays(days != null ? days : 7);
        
        List<ProxyDelayHistory> histories;
        if (proxyName != null && !proxyName.isEmpty()) {
            histories = proxyDelayHistoryService.getHistoriesByProxyNameAndTimeRange(
                    groupName, proxyName, startTime, endTime);
        } else {
            histories = proxyDelayHistoryService.getHistoriesByTimeRange(groupName, startTime, endTime);
        }
        
        Map<String, Object> chartData = proxyDelayHistoryService.convertToChartData(histories);
        return ResponseEntity.ok(chartData);
    }

    @GetMapping("/node-history")
    public String showNodeHistory(@RequestParam String group, @RequestParam String node, Model model) {
        // 获取代理组配置
        MonitoredProxyGroup config = monitoredProxyGroupService.getConfigByGroupName(group).orElse(null);
        if (config == null) {
            model.addAttribute("error", "代理组 " + group + " 不存在");
            return "error";
        }
        
        model.addAttribute("groupName", group);
        model.addAttribute("nodeName", node);
        model.addAttribute("config", config);
        
        return "node-history";
    }

    @GetMapping("/api/node-history-data")
    @ResponseBody
    public Map<String, Object> getNodeHistoryData(@RequestParam String group, 
                                                  @RequestParam String node, 
                                                  @RequestParam(defaultValue = "7") int days) {
        
        // 计算时间范围
        LocalDateTime endTime = LocalDateTime.now();
        LocalDateTime startTime = endTime.minusDays(days);
        
        // 获取延迟历史数据
        List<ProxyDelayHistory> historyList = proxyDelayHistoryService.findByGroupNameAndProxyNameAndTimeRange(
            group, node, startTime, endTime);
        
        // 准备返回数据
        Map<String, Object> result = new HashMap<>();
        List<String> timeLabels = new ArrayList<>();
        List<Integer> delayValues = new ArrayList<>();
        
        // 如果有数据，按时间排序
        if (!historyList.isEmpty()) {
            historyList.sort(Comparator.comparing(ProxyDelayHistory::getTestTime));
            
            for (ProxyDelayHistory history : historyList) {
                timeLabels.add(history.getTestTime().toString());
                delayValues.add(history.getDelay());
            }
        }
        
        result.put("timeLabels", timeLabels);
        result.put("delayValues", delayValues);
        result.put("groupName", group);
        result.put("nodeName", node);
        result.put("days", days);
        
        return result;
    }

    @GetMapping("/api/proxy-stats/{groupName}")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> getProxyStats(@PathVariable String groupName,
                                                         @RequestParam(defaultValue = "7") int days) {
        
        LocalDateTime endTime = LocalDateTime.now();
        LocalDateTime startTime = endTime.minusDays(days);
        
        // 获取该代理组的所有历史记录
        List<ProxyDelayHistory> histories = proxyDelayHistoryService.getHistoriesByTimeRange(groupName, startTime, endTime);
        
        // 按代理节点分组计算统计信息
        Map<String, List<ProxyDelayHistory>> groupedByProxy = histories.stream()
                .collect(Collectors.groupingBy(ProxyDelayHistory::getProxyName));
        
        Map<String, Map<String, Object>> proxyStats = new HashMap<>();
        
        for (Map.Entry<String, List<ProxyDelayHistory>> entry : groupedByProxy.entrySet()) {
            String proxyName = entry.getKey();
            List<ProxyDelayHistory> proxyHistories = entry.getValue();
            
            // 计算统计信息
            List<Integer> connectedDelays = proxyHistories.stream()
                    .map(ProxyDelayHistory::getDelay)
                    .filter(delay -> delay >= 0)
                    .collect(Collectors.toList());
            
            int totalCount = proxyHistories.size();
            int connectedCount = connectedDelays.size();
            double connectivityRate = totalCount > 0 ? (double) connectedCount / totalCount * 100 : 0;
            
            Map<String, Object> stats = new HashMap<>();
            stats.put("connectivityRate", Math.round(connectivityRate));
            stats.put("totalTests", totalCount);
            stats.put("connectedTests", connectedCount);
            
            if (!connectedDelays.isEmpty()) {
                stats.put("avgDelay", connectedDelays.stream().mapToInt(Integer::intValue).average().orElse(0));
                stats.put("minDelay", connectedDelays.stream().mapToInt(Integer::intValue).min().orElse(0));
                stats.put("maxDelay", connectedDelays.stream().mapToInt(Integer::intValue).max().orElse(0));
            } else {
                stats.put("avgDelay", null);
                stats.put("minDelay", null);
                stats.put("maxDelay", null);
            }
            
            proxyStats.put(proxyName, stats);
        }
        
        Map<String, Object> result = new HashMap<>();
        result.put("proxyStats", proxyStats);
        result.put("groupName", groupName);
        result.put("days", days);
        
        return ResponseEntity.ok(result);
    }
}
 