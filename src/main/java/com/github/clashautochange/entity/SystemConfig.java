package com.github.clashautochange.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 系统配置实体类
 * 用于存储系统级别的配置项，如Clash API设置
 */
@Entity
@Table(name = "system_config")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class SystemConfig {

    /**
     * 配置键
     */
    @Id
    @Column(nullable = false)
    private String configKey;

    /**
     * 配置值
     */
    @Column(nullable = false)
    private String configValue;

    /**
     * 配置描述
     */
    @Column
    private String description;
} 