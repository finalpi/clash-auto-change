package com.github.clashautochange.controller;

import com.github.clashautochange.model.ClashDelayResponse;
import com.github.clashautochange.model.ClashProxiesResponse;
import com.github.clashautochange.model.ClashSelectProxyRequest;
import com.github.clashautochange.service.ClashApiService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/clash")
public class ClashController {

    private final ClashApiService clashApiService;

    @Autowired
    public ClashController(ClashApiService clashApiService) {
        this.clashApiService = clashApiService;
    }

    @GetMapping("/proxies")
    public ResponseEntity<ClashProxiesResponse> getAllProxies() {
        return ResponseEntity.ok(clashApiService.getAllProxies());
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