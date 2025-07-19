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
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

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
}
 