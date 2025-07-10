package com.github.clashautochange.controller;

import com.github.clashautochange.model.ClashDelayResponse;
import com.github.clashautochange.model.ClashProxiesResponse;
import com.github.clashautochange.model.ClashSelectProxyRequest;
import com.github.clashautochange.service.ClashApiService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.client.ResourceAccessException;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/clash")
@Slf4j
public class ClashController {

    private final ClashApiService clashApiService;

    @Autowired
    public ClashController(ClashApiService clashApiService) {
        this.clashApiService = clashApiService;
    }

    @GetMapping("/proxies")
    public ResponseEntity<?> getAllProxies() {
        try {
            log.info("接收到获取所有代理的请求");
            ClashProxiesResponse response = clashApiService.getAllProxies();
            log.info("成功获取到所有代理，返回代理数量: {}", 
                    response != null && response.getProxies() != null ? response.getProxies().size() : 0);
            return ResponseEntity.ok(response);
        } catch (ResourceAccessException e) {
            log.error("无法连接到Clash API: {}", e.getMessage());
            HashMap<String, String> error = new HashMap<>();
            error.put("error", "无法连接到Clash API，请检查API设置和Clash状态");
            return ResponseEntity.status(503).body(error);
        } catch (Exception e) {
            log.error("获取代理时出错: {}", e.getMessage(), e);
            HashMap<String, String> error = new HashMap<>();
            error.put("error", "获取代理失败: " + e.getMessage());
            return ResponseEntity.badRequest().body(error);
        }
    }
    
    @GetMapping("/test-connection")
    public ResponseEntity<?> testConnection() {
        try {
            log.info("接收到测试连接请求");
            Map<String, Object> result = new HashMap<>();
            
            // 尝试获取代理信息
            ClashProxiesResponse response = clashApiService.getAllProxies();
            
            // 如果成功获取到代理信息，返回成功状态
            result.put("status", "success");
            result.put("message", "连接成功");
            result.put("proxyCount", response != null && response.getProxies() != null ? 
                    response.getProxies().size() : 0);
            
            log.info("测试连接成功，代理数量: {}", result.get("proxyCount"));
            return ResponseEntity.ok(result);
        } catch (ResourceAccessException e) {
            log.error("测试连接失败 - 无法连接到Clash API: {}", e.getMessage());
            Map<String, Object> error = new HashMap<>();
            error.put("status", "error");
            error.put("message", "无法连接到Clash API，请检查API设置和Clash状态");
            return ResponseEntity.status(503).body(error);
        } catch (Exception e) {
            log.error("测试连接失败: {}", e.getMessage(), e);
            Map<String, Object> error = new HashMap<>();
            error.put("status", "error");
            error.put("message", "测试连接失败: " + e.getMessage());
            return ResponseEntity.badRequest().body(error);
        }
    }

    @GetMapping("/proxies/{proxyName}/delay")
    public ResponseEntity<ClashDelayResponse> testProxyDelay(
            @PathVariable String proxyName,
            @RequestParam String url,
            @RequestParam(defaultValue = "5000") Integer timeout) {
        return ResponseEntity.ok(clashApiService.testProxyDelay(proxyName, url, timeout));
    }

    @PutMapping("/proxies/{proxyGroup}")
    public ResponseEntity<Void> selectProxy(
            @PathVariable String proxyGroup,
            @RequestBody ClashSelectProxyRequest request) {
        clashApiService.selectProxy(proxyGroup, request.getName());
        return ResponseEntity.ok().build();
    }
} 