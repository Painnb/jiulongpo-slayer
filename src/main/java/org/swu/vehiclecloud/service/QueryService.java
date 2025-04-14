package org.swu.vehiclecloud.service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

/**
 * 查询服务接口
 * 提供查询异常数据的功能
 */
public interface QueryService {
    
    /**
     * 查询指定表的所有数据
     * @param tableName 表名
     * @return 查询结果列表
     */
    List<Map<String, Object>> queryTable(String tableName);
    
    /**
     * 查询指定车辆和时间范围内的组合数据
     * @param vehicleId 车辆ID
     * @param startTime 开始时间
     * @param endTime 结束时间
     * @param selectedTables 选定的异常表
     * @param selectedColumns 每个表选定的列
     * @return 查询结果列表
     */
    List<Map<String, Object>> queryCombinedData(
            String vehicleId, 
            LocalDateTime startTime, 
            LocalDateTime endTime, 
            List<String> selectedTables,
            Map<String, List<String>> selectedColumns);
            
    /**
     * 查询时间段内所有车辆的异常数据
     * @param startTime 开始时间
     * @param endTime 结束时间
     * @param selectedTables 选定的异常表
     * @param selectedColumns 每个表选定的列
     * @return 查询结果列表
     */
    List<Map<String, Object>> queryAllVehiclesExceptions(
            LocalDateTime startTime,
            LocalDateTime endTime,
            List<String> selectedTables,
            Map<String, List<String>> selectedColumns);
}