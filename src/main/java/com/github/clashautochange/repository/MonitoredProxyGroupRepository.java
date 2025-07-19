package com.github.clashautochange.repository;

import com.github.clashautochange.entity.MonitoredProxyGroup;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * 监控代理组存储库接口
 */
@Repository
public interface MonitoredProxyGroupRepository extends JpaRepository<MonitoredProxyGroup, Long> {
    
    /**
     * 根据策略组名称查找配置
     * 
     * @param groupName 策略组名称
     * @return 监控代理组配置
     */
    Optional<MonitoredProxyGroup> findByGroupName(String groupName);
    
    /**
     * 查找所有已启用的配置
     * 
     * @return 已启用的监控代理组配置列表
     */
    List<MonitoredProxyGroup> findByEnabledTrue();
} 