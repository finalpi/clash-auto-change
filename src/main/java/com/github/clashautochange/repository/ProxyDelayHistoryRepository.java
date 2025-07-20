package com.github.clashautochange.repository;

import com.github.clashautochange.entity.ProxyDelayHistory;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 代理延迟历史记录存储库接口
 */
@Repository
public interface ProxyDelayHistoryRepository extends JpaRepository<ProxyDelayHistory, Long> {

    /**
     * 根据代理组名称查找历史记录
     *
     * @param groupName 代理组名称
     * @return 代理延迟历史记录列表
     */
    List<ProxyDelayHistory> findByGroupNameOrderByTestTimeDesc(String groupName);

    /**
     * 根据代理组名称和代理节点名称查找历史记录
     *
     * @param groupName 代理组名称
     * @param proxyName 代理节点名称
     * @return 代理延迟历史记录列表
     */
    List<ProxyDelayHistory> findByGroupNameAndProxyNameOrderByTestTimeDesc(String groupName, String proxyName);

    /**
     * 查找指定时间范围内的历史记录
     *
     * @param groupName 代理组名称
     * @param startTime 开始时间
     * @param endTime 结束时间
     * @return 代理延迟历史记录列表
     */
    List<ProxyDelayHistory> findByGroupNameAndTestTimeBetweenOrderByTestTimeAsc(
            String groupName, LocalDateTime startTime, LocalDateTime endTime);

    /**
     * 查找指定时间范围内特定代理节点的历史记录
     *
     * @param groupName 代理组名称
     * @param proxyName 代理节点名称
     * @param startTime 开始时间
     * @param endTime 结束时间
     * @return 代理延迟历史记录列表
     */
    List<ProxyDelayHistory> findByGroupNameAndProxyNameAndTestTimeBetweenOrderByTestTimeAsc(
            String groupName, String proxyName, LocalDateTime startTime, LocalDateTime endTime);

    /**
     * 删除指定时间之前的历史记录
     *
     * @param dateTime 截止时间
     * @return 删除的记录数
     */
    long deleteByTestTimeBefore(LocalDateTime dateTime);

    /**
     * 查询指定代理组中的所有唯一代理节点名称
     *
     * @param groupName 代理组名称
     * @return 代理节点名称列表
     */
    @Query("SELECT DISTINCT p.proxyName FROM ProxyDelayHistory p WHERE p.groupName = :groupName")
    List<String> findDistinctProxyNamesByGroupName(@Param("groupName") String groupName);

    List<ProxyDelayHistory> findByGroupNameAndTestTimeBetween(String groupName, LocalDateTime startTime, LocalDateTime endTime);

    List<ProxyDelayHistory> findByGroupNameAndProxyNameAndTestTimeBetween(String groupName, String proxyName, LocalDateTime startTime, LocalDateTime endTime);

    /**
     * 查找指定时间范围内特定代理节点的历史记录（按时间倒序）
     *
     * @param groupName 代理组名称
     * @param proxyName 代理节点名称
     * @param startTime 开始时间
     * @param endTime 结束时间
     * @return 代理延迟历史记录列表
     */
    List<ProxyDelayHistory> findByGroupNameAndProxyNameAndTestTimeBetweenOrderByTestTimeDesc(
            String groupName, String proxyName, LocalDateTime startTime, LocalDateTime endTime);
}