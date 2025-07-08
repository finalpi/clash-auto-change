package com.github.clashautochange.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ClashDelayRequest {
    private String url;
    private Integer timeout;
} 