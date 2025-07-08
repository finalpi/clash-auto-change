package com.github.clashautochange.controller;

import com.github.clashautochange.entity.ProxyGroupConfig;
import com.github.clashautochange.service.ClashApiService;
import com.github.clashautochange.service.ProxyGroupConfigService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.client.ResourceAccessException;

import java.util.Collections;
import java.util.List;

/**
 * 策略组配置控制器
 */
@RestController
@RequestMapping("/api/config")
@Slf4j
public class ProxyGroupConfigController {

    private final ProxyGroupConfigService proxyGroupConfigService;
    private final ClashApiService clashApiService;

    @Autowired
    public ProxyGroupConfigController(ProxyGroupConfigService proxyGroupConfigService, 
                                     ClashApiService clashApiService) {
        this.proxyGroupConfigService = proxyGroupConfigService;
        this.clashApiService = clashApiService;
    }

    /**
     * 获取所有策略组配置
     * 
     * @return 策略组配置列表
     */
    @GetMapping
    public ResponseEntity<List<ProxyGroupConfig>> getAllConfigs() {
        return ResponseEntity.ok(proxyGroupConfigService.getAllConfigs());
    }

    /**
     * 根据ID获取策略组配置
     * 
     * @param id 配置ID
     * @return 策略组配置
     */
    @GetMapping("/{id}")
    public ResponseEntity<ProxyGroupConfig> getConfigById(@PathVariable Long id) {
        return proxyGroupConfigService.getConfigById(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    /**
     * 创建或更新策略组配置
     * 
     * @param config 策略组配置
     * @return 保存后的配置
     */
    @PostMapping
    public ResponseEntity<ProxyGroupConfig> saveConfig(@RequestBody ProxyGroupConfig config) {
        return ResponseEntity.ok(proxyGroupConfigService.saveConfig(config));
    }

    /**
     * 删除策略组配置
     * 
     * @param id 配置ID
     * @return 无内容响应
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteConfig(@PathVariable Long id) {
        proxyGroupConfigService.deleteConfig(id);
        return ResponseEntity.noContent().build();
    }
    
    /**
     * 获取特定策略组的代理节点
     * 
     * @param groupName 策略组名称
     * @return 代理节点列表
     */
    @GetMapping("/group-proxies/{groupName}")
    public ResponseEntity<List<String>> getGroupProxies(@PathVariable String groupName) {
        log.info("接收到获取策略组 '{}' 的代理节点请求", groupName);
        
        if (groupName == null || groupName.trim().isEmpty()) {
            log.warn("策略组名称为空");
            return ResponseEntity.badRequest().body(Collections.singletonList("策略组名称不能为空"));
        }
        
        try {
            log.info("调用ClashApiService.getGroupProxies获取代理节点");
            List<String> proxies = clashApiService.getGroupProxies(groupName);
            
            if (proxies == null) {
                log.warn("返回的代理节点列表为null");
                return ResponseEntity.ok(Collections.emptyList());
            }
            
            log.info("成功获取策略组 '{}' 的代理节点，共 {} 个", groupName, proxies.size());
            return ResponseEntity.ok(proxies);
        } catch (ResourceAccessException e) {
            log.error("无法连接到Clash API: {}", e.getMessage());
            return ResponseEntity.status(503).body(Collections.singletonList("无法连接到Clash API，请检查API设置和Clash状态"));
        } catch (Exception e) {
            log.error("获取策略组 '{}' 的代理节点时出错: {}", groupName, e.getMessage(), e);
            return ResponseEntity.badRequest().body(Collections.singletonList("获取代理节点失败: " + e.getMessage()));
        }
    }
} 