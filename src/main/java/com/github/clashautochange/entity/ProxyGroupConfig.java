package com.github.clashautochange.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 策略组配置实体类
 * 用于存储策略组及其优先节点的配置
 */
@Entity
@Table(name = "proxy_group_config")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class ProxyGroupConfig {

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
     * 优先节点名称
     */
    @Column(nullable = false)
    private String preferredProxy;

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
     * 最大可接受延迟（毫秒）
     */
    @Column(nullable = false)
    private Integer maxDelay;

    /**
     * 最大超时次数，超过此次数才会切换节点
     */
    @Column(columnDefinition = "integer default 3")
    private Integer maxTimeoutCount = 3; // 默认值为3次

    /**
     * 当前超时计数，记录连续超时的次数
     */
    @Column(columnDefinition = "integer default 0")
    private Integer currentTimeoutCount = 0;

    /**
     * 是否启用
     */
    @Column(nullable = false)
    private Boolean enabled;
} 