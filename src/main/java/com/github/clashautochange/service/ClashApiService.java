package com.github.clashautochange.service;

import com.github.clashautochange.config.ClashApiConfig;
import com.github.clashautochange.model.ClashDelayRequest;
import com.github.clashautochange.model.ClashDelayResponse;
import com.github.clashautochange.model.ClashProxiesResponse;
import com.github.clashautochange.model.ClashSelectProxyRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Clash API 服务
 * 提供与Clash API交互的方法
 */
@Service
@Slf4j
public class ClashApiService {

    private final RestTemplate restTemplate;
    private final ClashApiConfig clashApiConfig;

    @Autowired
    public ClashApiService(RestTemplate restTemplate, ClashApiConfig clashApiConfig) {
        this.restTemplate = restTemplate;
        this.clashApiConfig = clashApiConfig;
    }

    /**
     * 获取请求头
     *
     * @return HTTP请求头
     */
    private HttpHeaders getHeaders() {
        HttpHeaders headers = new HttpHeaders();
        String secret = clashApiConfig.getSecret();
        if (secret != null && !secret.isEmpty()) {
            headers.set("Authorization", "Bearer " + secret);
        }
        return headers;
    }

    /**
     * 获取所有代理
     *
     * @return 代理响应
     */
    public ClashProxiesResponse getAllProxies() {
        HttpEntity<Void> requestEntity = new HttpEntity<>(getHeaders());
        ResponseEntity<ClashProxiesResponse> response = restTemplate.exchange(
                clashApiConfig.getBaseUrl() + "/proxies",
                HttpMethod.GET,
                requestEntity,
                ClashProxiesResponse.class
        );
        return response.getBody();
    }

    /**
     * 获取所有代理组
     *
     * @return 代理组名称和其代理节点的映射
     */
    public Map<String, List<String>> getAllGroups() {
        HttpEntity<Void> requestEntity = new HttpEntity<>(getHeaders());
        ResponseEntity<Map> response = restTemplate.exchange(
                clashApiConfig.getBaseUrl() + "/group",
                HttpMethod.GET,
                requestEntity,
                Map.class
        );
        
        Map<String, List<String>> result = new HashMap<>();
        if (response.getBody() != null && response.getBody().containsKey("proxies")) {
            List<Map<String, Object>> proxies = (List<Map<String, Object>>) response.getBody().get("proxies");
            for (Map<String, Object> proxy : proxies) {
                String name = (String) proxy.get("name");
                String type = (String) proxy.get("type");
                
                if ("Selector".equals(type) && proxy.containsKey("all")) {
                    List<String> all = (List<String>) proxy.get("all");
                    result.put(name, all);
                }
            }
        }
        
        return result;
    }

    /**
     * 获取特定策略组的代理节点
     *
     * @param groupName 策略组名称
     * @return 代理节点列表
     */
    public List<String> getGroupProxies(String groupName) {
        log.info("获取策略组 '{}' 的代理节点", groupName);
        
        try {
            HttpEntity<Void> requestEntity = new HttpEntity<>(getHeaders());
            String url = clashApiConfig.getBaseUrl() + "/proxies/" + groupName;
            log.info("请求URL: {}", url);
            
            ResponseEntity<Map> response = restTemplate.exchange(
                    url,
                    HttpMethod.GET,
                    requestEntity,
                    Map.class
            );
            
            List<String> result = new ArrayList<>();
            if (response.getBody() != null) {
                log.info("API响应状态码: {}", response.getStatusCode());
                log.info("API响应内容: {}", response.getBody());
                
                if (response.getBody().containsKey("all")) {
                    result = (List<String>) response.getBody().get("all");
                    log.info("从 'all' 字段获取到 {} 个代理节点", result.size());
                } 
                else if (response.getBody().containsKey("proxies")) {
                    List<Map<String, Object>> proxies = (List<Map<String, Object>>) response.getBody().get("proxies");
                    if (proxies != null && !proxies.isEmpty()) {
                        for (Map<String, Object> proxy : proxies) {
                            if (proxy.containsKey("name")) {
                                result.add((String) proxy.get("name"));
                            }
                        }
                        log.info("从 'proxies' 字段获取到 {} 个代理节点", result.size());
                    }
                } 
                else if (response.getBody().containsKey("now") && response.getBody().containsKey("all")) {
                    result = (List<String>) response.getBody().get("all");
                    log.info("从响应的 'all' 字段获取到 {} 个代理节点", result.size());
                }
                else {
                    for (Object entryObj : response.getBody().entrySet()) {
                        if (entryObj instanceof Map.Entry) {
                            Map.Entry<?, ?> entry = (Map.Entry<?, ?>) entryObj;
                            log.info("响应体键值对: {} = {}", entry.getKey(), entry.getValue());
                            if (entry.getKey() instanceof String && entry.getValue() instanceof String) {
                                result.add((String) entry.getKey());
                            }
                        }
                    }
                    log.info("从响应体直接解析获取到 {} 个代理节点", result.size());
                }
            } else {
                log.warn("API响应为空");
            }
            
            log.info("最终返回 {} 个代理节点: {}", result.size(), result);
            return result;
        } catch (Exception e) {
            log.error("获取策略组代理节点时出错: {}", e.getMessage(), e);
            throw e;
        }
    }

    /**
     * 测试代理延迟
     *
     * @param proxyName 代理名称
     * @param url 测试URL
     * @param timeout 超时时间
     * @return 延迟响应
     */
    public ClashDelayResponse testProxyDelay(String proxyName, String url, Integer timeout) {
        HttpEntity<Void> requestEntity = new HttpEntity<>(getHeaders());

        UriComponentsBuilder builder = UriComponentsBuilder
                .fromHttpUrl(clashApiConfig.getBaseUrl() + "/proxies/" + proxyName + "/delay")
                .queryParam("url", url)
                .queryParam("timeout", timeout);

        ResponseEntity<ClashDelayResponse> response = restTemplate.exchange(
                builder.toUriString(),
                HttpMethod.GET,
                requestEntity,
                ClashDelayResponse.class
        );
        return response.getBody();
    }

    /**
     * 选择代理
     *
     * @param proxyGroup 代理组
     * @param proxyName 代理名称
     */
    public void selectProxy(String proxyGroup, String proxyName) {
        log.info("为策略组 '{}' 选择代理 '{}'", proxyGroup, proxyName);
        HttpEntity<ClashSelectProxyRequest> requestEntity = new HttpEntity<>(
                new ClashSelectProxyRequest(proxyName),
                getHeaders()
        );

        try {
            String url = clashApiConfig.getBaseUrl() + "/proxies/" + proxyGroup;
            log.info("请求URL: {}, 请求体: {}", url, new ClashSelectProxyRequest(proxyName));
            
            ResponseEntity<Void> response = restTemplate.exchange(
                    url,
                    HttpMethod.PUT,
                    requestEntity,
                    Void.class
            );
            
            log.info("选择代理成功，API响应状态码: {}", response.getStatusCode());
        } catch (Exception e) {
            log.error("选择代理时出错: {}", e.getMessage(), e);
            throw e;
        }
    }

    /**
     * 测试策略组中所有代理的延迟
     *
     * @param groupName 策略组名称
     * @param url 测试URL
     * @param timeout 超时时间
     * @return 代理名称和延迟的映射
     */
    public Map<String, Integer> testGroupDelay(String groupName, String url, Integer timeout) {
        log.info("测试策略组 '{}' 中所有代理的延迟", groupName);
        HttpEntity<Void> requestEntity = new HttpEntity<>(getHeaders());

        UriComponentsBuilder builder = UriComponentsBuilder
                .fromHttpUrl(clashApiConfig.getBaseUrl() + "/group/" + groupName + "/delay")
                .queryParam("url", url)
                .queryParam("timeout", timeout);
                
        String requestUrl = builder.toUriString();
        log.info("请求URL: {}", requestUrl);

        try {
            ResponseEntity<Map> response = restTemplate.exchange(
                    requestUrl,
                    HttpMethod.GET,
                    requestEntity,
                    Map.class
            );
            
            log.info("API响应状态码: {}", response.getStatusCode());
            
            Map<String, Integer> result = new HashMap<>();
            if (response.getBody() != null) {
                log.info("API响应内容: {}", response.getBody());
                
                // 解析响应，提取每个代理的延迟
                Map<String, Object> responseBody = response.getBody();
                for (Map.Entry<String, Object> entry : responseBody.entrySet()) {
                    if (entry.getValue() instanceof Integer) {
                        result.put(entry.getKey(), (Integer) entry.getValue());
                    } else if (entry.getValue() instanceof Number) {
                        result.put(entry.getKey(), ((Number) entry.getValue()).intValue());
                    }
                }
                
                log.info("解析得到 {} 个代理的延迟结果", result.size());
            } else {
                log.warn("API响应为空");
            }

            return result;
        } catch (Exception e) {
            log.error("测试策略组代理延迟时出错: {}", e.getMessage(), e);
            return new HashMap<>();
        }
    }

    /**
     * 获取特定策略组的信息
     *
     * @param groupName 策略组名称
     * @return 策略组信息
     */
    public Map<String, Object> getGroupInfo(String groupName) {
        log.info("获取策略组 '{}' 的信息", groupName);
        
        try {
            HttpEntity<Void> requestEntity = new HttpEntity<>(getHeaders());
            String url = clashApiConfig.getBaseUrl() + "/group/" + groupName;
            log.info("请求URL: {}", url);
            
            ResponseEntity<Map> response = restTemplate.exchange(
                    url,
                    HttpMethod.GET,
                    requestEntity,
                    Map.class
            );
            
            if (response.getBody() != null) {
                log.info("API响应状态码: {}", response.getStatusCode());
                log.debug("API响应内容: {}", response.getBody());
                
                Map<String, Object> groupInfo = response.getBody();
                log.info("获取到策略组 '{}' 的信息，当前选中: {}", groupName, groupInfo.get("now"));
                return groupInfo;
            } else {
                log.warn("API响应为空");
                return new HashMap<>();
            }
        } catch (Exception e) {
            log.error("获取策略组 '{}' 的信息时出错: {}", groupName, e.getMessage(), e);
            return new HashMap<>();
        }
    }
}