package com.github.clashautochange.repository;

import com.github.clashautochange.entity.ProxyGroupConfig;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * 策略组配置存储库接口
 */
@Repository
public interface ProxyGroupConfigRepository extends JpaRepository<ProxyGroupConfig, Long> {
    
    /**
     * 根据策略组名称查找配置
     * 
     * @param groupName 策略组名称
     * @return 策略组配置
     */
    Optional<ProxyGroupConfig> findByGroupName(String groupName);
    
    /**
     * 查找所有已启用的配置
     * 
     * @return 已启用的策略组配置列表
     */
    List<ProxyGroupConfig> findByEnabledTrue();
} 