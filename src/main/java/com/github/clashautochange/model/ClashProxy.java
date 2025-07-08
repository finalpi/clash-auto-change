package com.github.clashautochange.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Data;

import java.util.List;
import java.util.Map;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class ClashProxy {
    private String name;
    private String type;
    private String udp;
    private Map<String, Object> history;
    private List<Map<String, Object>> all;
    private String now;
} 