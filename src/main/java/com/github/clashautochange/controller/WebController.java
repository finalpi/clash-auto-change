package com.github.clashautochange.controller;

import com.github.clashautochange.entity.ProxyGroupConfig;
import com.github.clashautochange.model.ClashProxiesResponse;
import com.github.clashautochange.service.ClashApiService;
import com.github.clashautochange.service.ProxyGroupConfigService;
import com.github.clashautochange.service.SystemConfigService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Web控制器
 * 处理前端页面请求
 */
@Controller
@Slf4j
public class WebController {

    private final SystemConfigService systemConfigService;
    private final ProxyGroupConfigService proxyGroupConfigService;
    private final ClashApiService clashApiService;

    @Autowired
    public WebController(SystemConfigService systemConfigService, 
                         ProxyGroupConfigService proxyGroupConfigService,
                         ClashApiService clashApiService) {
        this.systemConfigService = systemConfigService;
        this.proxyGroupConfigService = proxyGroupConfigService;
        this.clashApiService = clashApiService;
    }

    /**
     * 首页
     * 
     * @param model 模型
     * @return 首页视图
     */
    @GetMapping("/")
    public String index(Model model) {
        try {
            model.addAttribute("apiConfig", systemConfigService.getClashApiConfig());
        } catch (Exception e) {
            log.error("获取API配置失败", e);
            model.addAttribute("error", "获取API配置失败: " + e.getMessage());
            Map<String, String> defaultConfig = new HashMap<>();
            defaultConfig.put("baseUrl", "配置错误");
            defaultConfig.put("secret", "配置错误");
            model.addAttribute("apiConfig", defaultConfig);
        }
        return "index";
    }

    /**
     * 系统设置页面
     * 
     * @param model 模型
     * @return 系统设置视图
     */
    @GetMapping("/settings")
    public String settings(Model model) {
        try {
            model.addAttribute("apiConfig", systemConfigService.getClashApiConfig());
        } catch (Exception e) {
            log.error("获取API配置失败", e);
            model.addAttribute("error", "获取API配置失败: " + e.getMessage());
            Map<String, String> defaultConfig = new HashMap<>();
            defaultConfig.put("baseUrl", "");
            defaultConfig.put("secret", "");
            model.addAttribute("apiConfig", defaultConfig);
        }
        return "settings";
    }

    /**
     * 更新系统设置
     * 
     * @param baseUrl Clash API基础URL
     * @param secret Clash API密钥
     * @param redirectAttributes 重定向属性
     * @return 重定向到设置页面
     */
    @PostMapping("/settings/update")
    public String updateSettings(@RequestParam String baseUrl, 
                                @RequestParam String secret,
                                RedirectAttributes redirectAttributes) {
        try {
            systemConfigService.updateClashApiConfig(baseUrl, secret);
            redirectAttributes.addFlashAttribute("message", "系统设置已更新");
        } catch (Exception e) {
            log.error("更新API配置失败", e);
            redirectAttributes.addFlashAttribute("error", "更新API配置失败: " + e.getMessage());
        }
        return "redirect:/settings";
    }

    /**
     * 策略组配置页面
     * 
     * @param model 模型
     * @return 策略组配置视图
     */
    @GetMapping("/proxy-groups")
    public String proxyGroups(Model model) {
        try {
            List<ProxyGroupConfig> configs = proxyGroupConfigService.getAllConfigs();
            model.addAttribute("configs", configs);
            
            // 获取可用的策略组列表
            try {
                Map<String, List<String>> availableGroups = clashApiService.getAllGroups();
                model.addAttribute("availableGroups", availableGroups);
            } catch (ResourceAccessException e) {
                log.error("无法连接到Clash API", e);
                model.addAttribute("error", "无法连接到Clash API，请检查API设置和Clash状态");
                model.addAttribute("availableGroups", Collections.emptyMap());
            } catch (Exception e) {
                log.error("获取代理信息失败", e);
                model.addAttribute("error", "获取代理信息失败: " + e.getMessage());
                model.addAttribute("availableGroups", Collections.emptyMap());
            }
            
            model.addAttribute("newConfig", new ProxyGroupConfig());
        } catch (Exception e) {
            log.error("加载策略组配置页面失败", e);
            model.addAttribute("error", "加载策略组配置页面失败: " + e.getMessage());
            model.addAttribute("configs", Collections.emptyList());
            model.addAttribute("availableGroups", Collections.emptyMap());
            model.addAttribute("newConfig", new ProxyGroupConfig());
        }
        return "proxy-groups";
    }

    /**
     * 添加策略组配置
     * 
     * @param config 策略组配置
     * @param redirectAttributes 重定向属性
     * @return 重定向到策略组配置页面
     */
    @PostMapping("/proxy-groups/add")
    public String addProxyGroup(@ModelAttribute ProxyGroupConfig config,
                               RedirectAttributes redirectAttributes) {
        try {
            config.setEnabled(true);
            proxyGroupConfigService.saveConfig(config);
            redirectAttributes.addFlashAttribute("message", "策略组配置已添加");
        } catch (Exception e) {
            log.error("添加策略组配置失败", e);
            redirectAttributes.addFlashAttribute("error", "添加策略组配置失败: " + e.getMessage());
        }
        return "redirect:/proxy-groups";
    }

    // 编辑功能现在通过模态框实现，不再需要单独的编辑页面

    /**
     * 更新策略组配置
     * 
     * @param id 配置ID
     * @param config 策略组配置
     * @param redirectAttributes 重定向属性
     * @return 重定向到策略组配置页面
     */
    @PostMapping("/proxy-groups/update/{id}")
    public String updateProxyGroup(@PathVariable Long id, 
                                  @ModelAttribute ProxyGroupConfig config,
                                  RedirectAttributes redirectAttributes) {
        try {
            proxyGroupConfigService.getConfigById(id).ifPresent(existingConfig -> {
                existingConfig.setGroupName(config.getGroupName());
                existingConfig.setPreferredProxy(config.getPreferredProxy());
                existingConfig.setTestUrl(config.getTestUrl());
                existingConfig.setTimeout(config.getTimeout());
                existingConfig.setMaxDelay(config.getMaxDelay());
                existingConfig.setEnabled(config.getEnabled());
                proxyGroupConfigService.saveConfig(existingConfig);
                redirectAttributes.addFlashAttribute("message", "策略组配置已更新");
            });
        } catch (Exception e) {
            log.error("更新策略组配置失败", e);
            redirectAttributes.addFlashAttribute("error", "更新策略组配置失败: " + e.getMessage());
        }
        return "redirect:/proxy-groups";
    }

    /**
     * 删除策略组配置
     * 
     * @param id 配置ID
     * @param redirectAttributes 重定向属性
     * @return 重定向到策略组配置页面
     */
    @GetMapping("/proxy-groups/delete/{id}")
    public String deleteProxyGroup(@PathVariable Long id,
                                  RedirectAttributes redirectAttributes) {
        try {
            proxyGroupConfigService.deleteConfig(id);
            redirectAttributes.addFlashAttribute("message", "策略组配置已删除");
        } catch (Exception e) {
            log.error("删除策略组配置失败", e);
            redirectAttributes.addFlashAttribute("error", "删除策略组配置失败: " + e.getMessage());
        }
        return "redirect:/proxy-groups";
    }

    /**
     * 切换策略组配置启用状态
     * 
     * @param id 配置ID
     * @param redirectAttributes 重定向属性
     * @return 重定向到策略组配置页面
     */
    @GetMapping("/proxy-groups/toggle/{id}")
    public String toggleProxyGroup(@PathVariable Long id,
                                  RedirectAttributes redirectAttributes) {
        try {
            proxyGroupConfigService.getConfigById(id).ifPresent(config -> {
                config.setEnabled(!config.getEnabled());
                proxyGroupConfigService.saveConfig(config);
                redirectAttributes.addFlashAttribute("message", 
                        "策略组配置已" + (config.getEnabled() ? "启用" : "禁用"));
            });
        } catch (Exception e) {
            log.error("切换策略组配置状态失败", e);
            redirectAttributes.addFlashAttribute("error", "切换策略组配置状态失败: " + e.getMessage());
        }
        return "redirect:/proxy-groups";
    }
    
    /**
     * 处理全局异常
     * 
     * @param e 异常
     * @param model 模型
     * @return 错误页面
     */
    @ExceptionHandler(Exception.class)
    public String handleError(Exception e, Model model) {
        log.error("全局异常", e);
        model.addAttribute("error", "发生错误: " + e.getMessage());
        return "index";
    }
} 