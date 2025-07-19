package com.github.clashautochange.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 代理节点延迟历史记录实体类
 * 用于存储代理节点的延迟测试结果历史
 */
@Entity
@Table(name = "proxy_delay_history")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class ProxyDelayHistory {

    /**
     * 主键ID
     */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * 代理组名称
     */
    @Column(nullable = false)
    private String groupName;

    /**
     * 代理节点名称
     */
    @Column(nullable = false)
    private String proxyName;

    /**
     * 延迟时间（毫秒），-1 表示未连通
     */
    @Column(nullable = false)
    private Integer delay;

    /**
     * 测试时间
     */
    @Column(nullable = false)
    private LocalDateTime testTime;

    /**
     * 创建一个新的代理延迟历史记录
     * 
     * @param groupName 代理组名称
     * @param proxyName 代理节点名称
     * @param delay 延迟时间
     * @return 新的代理延迟历史记录
     */
    public static ProxyDelayHistory create(String groupName, String proxyName, Integer delay) {
        ProxyDelayHistory history = new ProxyDelayHistory();
        history.setGroupName(groupName);
        history.setProxyName(proxyName);
        history.setDelay(delay);
        history.setTestTime(LocalDateTime.now());
        return history;
    }
} 