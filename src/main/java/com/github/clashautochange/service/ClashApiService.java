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
     * 获取Clash版本信息
     * 
     * @return 版本信息Map
     */
    public Map<String, Object> getVersion() {
        HttpEntity<Void> requestEntity = new HttpEntity<>(getHeaders());
        log.info("获取Clash版本信息，请求URL: {}/version", clashApiConfig.getBaseUrl());
        
        ResponseEntity<Map> response = restTemplate.exchange(
                clashApiConfig.getBaseUrl() + "/version",
                HttpMethod.GET,
                requestEntity,
                Map.class
        );
        
        if (response.getBody() != null) {
            log.info("成功获取Clash版本信息: {}", response.getBody());
            return response.getBody();
        } else {
            log.warn("获取Clash版本信息返回空结果");
            return new HashMap<>();
        }
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
        try {
            HttpEntity<Void> requestEntity = new HttpEntity<>(getHeaders());
            String url = clashApiConfig.getBaseUrl() + "/proxies/" + groupName;
            
            ResponseEntity<Map> response = restTemplate.exchange(
                    url,
                    HttpMethod.GET,
                    requestEntity,
                    Map.class
            );
            
            List<String> result = new ArrayList<>();
            if (response.getBody() != null) {
                if (response.getBody().containsKey("all")) {
                    result = (List<String>) response.getBody().get("all");
                } 
                else if (response.getBody().containsKey("proxies")) {
                    List<Map<String, Object>> proxies = (List<Map<String, Object>>) response.getBody().get("proxies");
                    if (proxies != null && !proxies.isEmpty()) {
                        for (Map<String, Object> proxy : proxies) {
                            if (proxy.containsKey("name")) {
                                result.add((String) proxy.get("name"));
                            }
                        }
                    }
                } 
                else if (response.getBody().containsKey("now") && response.getBody().containsKey("all")) {
                    result = (List<String>) response.getBody().get("all");
                }
                else {
                    for (Object entryObj : response.getBody().entrySet()) {
                        if (entryObj instanceof Map.Entry) {
                            Map.Entry<?, ?> entry = (Map.Entry<?, ?>) entryObj;
                            if (entry.getKey() instanceof String && entry.getValue() instanceof String) {
                                result.add((String) entry.getKey());
                            }
                        }
                    }
                }
            }
            
            return result;
        } catch (Exception e) {
            log.error("获取策略组代理节点时出错: {}", e.getMessage());
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
        HttpEntity<ClashSelectProxyRequest> requestEntity = new HttpEntity<>(
                new ClashSelectProxyRequest(proxyName),
                getHeaders()
        );

        try {
            String url = clashApiConfig.getBaseUrl() + "/proxies/" + proxyGroup;
            
            ResponseEntity<Void> response = restTemplate.exchange(
                    url,
                    HttpMethod.PUT,
                    requestEntity,
                    Void.class
            );
        } catch (Exception e) {
            log.error("选择代理时出错: {}", e.getMessage());
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
        HttpEntity<Void> requestEntity = new HttpEntity<>(getHeaders());

        UriComponentsBuilder builder = UriComponentsBuilder
                .fromHttpUrl(clashApiConfig.getBaseUrl() + "/group/" + groupName + "/delay")
                .queryParam("url", url)
                .queryParam("timeout", timeout);
                
        String requestUrl = builder.toUriString();

        try {
            ResponseEntity<Map> response = restTemplate.exchange(
                    requestUrl,
                    HttpMethod.GET,
                    requestEntity,
                    Map.class
            );
            
            Map<String, Integer> result = new HashMap<>();
            if (response.getBody() != null) {
                // 解析响应，提取每个代理的延迟
                Map<String, Object> responseBody = response.getBody();
                for (Map.Entry<String, Object> entry : responseBody.entrySet()) {
                    if (entry.getValue() instanceof Integer) {
                        result.put(entry.getKey(), (Integer) entry.getValue());
                    } else if (entry.getValue() instanceof Number) {
                        result.put(entry.getKey(), ((Number) entry.getValue()).intValue());
                    }
                }
            }

            return result;
        } catch (Exception e) {
            log.error("测试策略组代理延迟时出错: {}", e.getMessage());
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
        try {
            HttpEntity<Void> requestEntity = new HttpEntity<>(getHeaders());
            String url = clashApiConfig.getBaseUrl() + "/proxies/" + groupName;
            
            ResponseEntity<Map> response = restTemplate.exchange(
                    url,
                    HttpMethod.GET,
                    requestEntity,
                    Map.class
            );
            
            if (response.getBody() != null) {
                Map<String, Object> groupInfo = response.getBody();
                return groupInfo;
            } else {
                return new HashMap<>();
            }
        } catch (Exception e) {
            log.error("获取策略组 '{}' 的信息时出错: {}", groupName, e.getMessage());
            return new HashMap<>();
        }
    }
}