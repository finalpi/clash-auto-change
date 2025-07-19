package com.github.clashautochange.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 监控代理组实体类
 * 用于存储需要被监控的代理组配置
 */
@Entity
@Table(name = "monitored_proxy_group")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class MonitoredProxyGroup {

    /**
     * 主键ID
     */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * 策略组名称
     */
    @Column(nullable = false, unique = true)
    private String groupName;

    /**
     * 测试URL
     */
    @Column(nullable = false)
    private String testUrl;

    /**
     * 超时时间（毫秒）
     */
    @Column(nullable = false)
    private Integer timeout;

    /**
     * 是否启用
     */
    @Column(nullable = false)
    private Boolean enabled;
} 