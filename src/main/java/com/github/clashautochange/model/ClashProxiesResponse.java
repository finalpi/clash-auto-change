package com.github.clashautochange.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Data;

import java.util.Map;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class ClashProxiesResponse {
    private Map<String, ClashProxy> proxies;
} 