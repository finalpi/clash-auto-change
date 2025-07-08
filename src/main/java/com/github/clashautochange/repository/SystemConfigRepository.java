package com.github.clashautochange.repository;

import com.github.clashautochange.entity.SystemConfig;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

/**
 * 系统配置存储库接口
 */
@Repository
public interface SystemConfigRepository extends JpaRepository<SystemConfig, String> {
    
    /**
     * 根据配置键查找配置
     * 
     * @param configKey 配置键
     * @return 系统配置
     */
    Optional<SystemConfig> findByConfigKey(String configKey);
} 