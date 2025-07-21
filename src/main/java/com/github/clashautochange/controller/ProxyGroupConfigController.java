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
    
    /**
     * 测试策略组的延迟
     * 
     * @param groupName 策略组名称
     * @param testUrl 测试URL
     * @param timeout 超时时间
     * @return 代理节点延迟映射
     */
    @GetMapping("/group-delays/{groupName}")
    public ResponseEntity<java.util.Map<String, Integer>> testGroupDelays(
            @PathVariable String groupName,
            @RequestParam(defaultValue = "https://www.gstatic.com/generate_204") String testUrl,
            @RequestParam(defaultValue = "5000") Integer timeout) {
        
        log.info("接收到测试策略组 '{}' 延迟的请求，测试URL: {}, 超时: {}ms", groupName, testUrl, timeout);
        
        if (groupName == null || groupName.trim().isEmpty()) {
            log.warn("策略组名称为空");
            return ResponseEntity.badRequest().body(java.util.Collections.singletonMap("error", -1));
        }
        
        try {
            // 先获取策略组中的所有节点
            List<String> allProxies = clashApiService.getGroupProxies(groupName);
            if (allProxies == null || allProxies.isEmpty()) {
                log.warn("策略组 '{}' 中没有找到代理节点", groupName);
                return ResponseEntity.ok(java.util.Collections.emptyMap());
            }
            
            log.info("调用ClashApiService.testGroupDelay测试延迟");
            java.util.Map<String, Integer> delays = clashApiService.testGroupDelay(groupName, testUrl, timeout);
            
            if (delays == null) {
                delays = new java.util.HashMap<>();
            }
            
            // 确保所有节点都有延迟信息，没有测试到的节点标记为离线
            java.util.Map<String, Integer> completeDelays = new java.util.HashMap<>();
            for (String proxy : allProxies) {
                Integer delay = delays.get(proxy);
                if (delay == null) {
                    // 没有测试到的节点标记为离线
                    completeDelays.put(proxy, -1);
                } else if (delay > 4000) {
                    // 大于4000ms的延迟标记为离线
                    completeDelays.put(proxy, -1);
                } else {
                    completeDelays.put(proxy, delay);
                }
            }
            
            log.info("成功测试策略组 '{}' 的延迟，共 {} 个代理节点", groupName, completeDelays.size());
            return ResponseEntity.ok(completeDelays);
        } catch (ResourceAccessException e) {
            log.error("无法连接到Clash API: {}", e.getMessage());
            return ResponseEntity.status(503).body(java.util.Collections.singletonMap("error", -1));
        } catch (Exception e) {
            log.error("测试策略组 '{}' 延迟时出错: {}", groupName, e.getMessage(), e);
            return ResponseEntity.badRequest().body(java.util.Collections.singletonMap("error", -1));
        }
    }
    
    /**
     * 获取所有策略组当前选择的节点
     * 
     * @return 策略组名称和当前选择节点的映射
     */
    @GetMapping("/current-proxies")
    public ResponseEntity<java.util.Map<String, String>> getCurrentProxies() {
        log.info("接收到获取所有策略组当前选择节点的请求");
        
        try {
            // 获取所有可用的策略组
            java.util.Map<String, List<String>> allGroups = clashApiService.getAllGroups();
            java.util.Map<String, String> currentProxies = new java.util.HashMap<>();
            
            if (allGroups != null) {
                for (String groupName : allGroups.keySet()) {
                    try {
                        java.util.Map<String, Object> groupInfo = clashApiService.getGroupInfo(groupName);
                        if (groupInfo != null && groupInfo.containsKey("now")) {
                            String currentProxy = (String) groupInfo.get("now");
                            currentProxies.put(groupName, currentProxy);
                        }
                    } catch (Exception e) {
                        log.warn("获取策略组 {} 当前选择节点失败: {}", groupName, e.getMessage());
                        currentProxies.put(groupName, "获取失败");
                    }
                }
            }
            
            log.info("成功获取 {} 个策略组的当前选择节点", currentProxies.size());
            return ResponseEntity.ok(currentProxies);
        } catch (ResourceAccessException e) {
            log.error("无法连接到Clash API: {}", e.getMessage());
            return ResponseEntity.status(503).body(java.util.Collections.singletonMap("error", "无法连接到Clash API"));
        } catch (Exception e) {
            log.error("获取当前选择节点时出错: {}", e.getMessage(), e);
            return ResponseEntity.badRequest().body(java.util.Collections.singletonMap("error", "获取失败"));
        }
    }
} 