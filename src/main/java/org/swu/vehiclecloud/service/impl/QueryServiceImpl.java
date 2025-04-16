package org.swu.vehiclecloud.service.impl;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.swu.vehiclecloud.mapper.ExcelMapper;
import org.swu.vehiclecloud.service.QueryService;
import org.swu.vehiclecloud.util.SQLInjectionProtector;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;

/**
 * 查询服务实现类
 * 该类实现了QueryService接口，提供查询数据库表数据的功能
 */
@Service
public class QueryServiceImpl implements QueryService {

    @Autowired
    private ExcelMapper excelMapper; // 复用ExcelMapper进行数据查询

    /**
     * 查询指定表的所有数据
     * @param tableName 表名，不能为空
     * @return 查询结果列表
     * @throws IllegalArgumentException 当表名验证失败时抛出
     */
    @Override
    public List<Map<String, Object>> queryTable(String tableName) {
        // SQL注入防护 - 验证表名合法性
        if (!SQLInjectionProtector.validateTableName(tableName)) {
            throw new IllegalArgumentException("非法的表名: " + tableName);
        }
        
        // 从数据库查询指定表的所有数据
        return excelMapper.selectAllFromTable(tableName);
    }

    /**
     * 查询指定车辆和时间范围内的组合数据
     * @param vehicleId 车辆ID，不能为空
     * @param startTime 开始时间，可为null
     * @param endTime 结束时间，可为null
     * @param selectedTables 选定的表名列表，不能为null或空
     * @param selectedColumns 每个表选定的列名映射，key为表名，value为列名列表
     * @return 查询结果列表
     * @throws IllegalArgumentException 当表名验证失败时抛出
     */
    @Override
    public List<Map<String, Object>> queryCombinedData(
            String vehicleId, 
            LocalDateTime startTime, 
            LocalDateTime endTime, 
            List<String> selectedTables,
            Map<String, List<String>> selectedColumns) {
        
        // 判断是否有有效的时间范围
        boolean hasTimeRange = startTime != null && endTime != null && startTime.isBefore(endTime);

        // 验证表名 - 防止SQL注入攻击
        for (String table : selectedTables) {
            if (!SQLInjectionProtector.validateTableName(table)) {
                throw new IllegalArgumentException("非法的表名: " + table);
            }
        }

        // 存储所有表的数据
        List<Map<String, Object>> combinedData = new ArrayList<>();
        
        // 遍历每个选定的表，根据是否有时间范围调用不同的查询方法
        for (String table : selectedTables) {
            List<Map<String, Object>> tableData;
            if (hasTimeRange) {
                // 有时间范围的情况：从每个表获取时间戳范围内的数据
                tableData = excelMapper.selectFromTableWithFilter(
                    table,
                    vehicleId,
                    startTime,
                    endTime
                );
            } else {
                // 无时间范围的情况：从每个表获取该车辆的所有记录
                tableData = excelMapper.selectByVehicleId(
                    table,
                    vehicleId
                );
            }
            
            // 处理查询结果，只保留选定的列
            List<String> columns = selectedColumns.getOrDefault(table, Collections.emptyList());
            if (!columns.isEmpty()) {
                for (Map<String, Object> row : tableData) {
                    Map<String, Object> filteredRow = new HashMap<>();
                    // 保留vehicleId和timestamp
                    filteredRow.put("vehicleId", row.get("vehicleId"));
                    if (row.containsKey("timestamp")) {
                        filteredRow.put("timestamp", row.get("timestamp"));
                    }
                    // 添加表名前缀，避免不同表的同名列冲突
                    for (String column : columns) {
                        if (row.containsKey(column)) {
                            filteredRow.put(table + "_" + column, row.get(column));
                        }
                    }
                    // 添加表名标记
                    filteredRow.put("table", table);
                    combinedData.add(filteredRow);
                }
            } else {
                // 如果没有选择列，则只添加标记列
                for (Map<String, Object> row : tableData) {
                    Map<String, Object> markerRow = new HashMap<>();
                    markerRow.put("vehicleId", row.get("vehicleId"));
                    if (row.containsKey("timestamp")) {
                        markerRow.put("timestamp", row.get("timestamp"));
                    }
                    markerRow.put("table", table);
                    markerRow.put(table, 1); // 标记存在
                    combinedData.add(markerRow);
                }
            }
        }
        
        return combinedData;
    }

    /**
     * 查询时间段内所有车辆的异常数据
     * @param startTime 开始时间
     * @param endTime 结束时间
     * @param selectedTables 选定的异常表
     * @param selectedColumns 每个表选定的列
     * @return 查询结果列表
     * @throws IllegalArgumentException 当表名验证失败时抛出
     */
    @Override
    public List<Map<String, Object>> queryAllVehiclesExceptions(
            LocalDateTime startTime,
            LocalDateTime endTime,
            List<String> selectedTables,
            Map<String, List<String>> selectedColumns) {
        
        // 验证时间范围
        if (startTime == null || endTime == null || !startTime.isBefore(endTime)) {
            throw new IllegalArgumentException("无效的时间范围");
        }
        
        // 验证表名 - 防止SQL注入攻击
        for (String table : selectedTables) {
            if (!SQLInjectionProtector.validateTableName(table)) {
                throw new IllegalArgumentException("非法的表名: " + table);
            }
        }
        
        // 存储所有表的数据
        List<Map<String, Object>> allVehiclesData = new ArrayList<>();
        
        // 遍历每个选定的表，查询时间范围内的所有车辆数据
        for (String table : selectedTables) {
            List<Map<String, Object>> tableData = excelMapper.selectAllVehiclesWithTimeRange(
                table,
                startTime,
                endTime
            );
            
            // 处理查询结果，只保留选定的列
            List<String> columns = selectedColumns.getOrDefault(table, Collections.emptyList());
            if (!columns.isEmpty()) {
                for (Map<String, Object> row : tableData) {
                    Map<String, Object> filteredRow = new HashMap<>();
                    // 保留vehicleId和timestamp
                    filteredRow.put("vehicleId", row.get("vehicleId"));
                    if (row.containsKey("timestamp")) {
                        filteredRow.put("timestamp", row.get("timestamp"));
                    }
                    // 添加表名前缀，避免不同表的同名列冲突
                    for (String column : columns) {
                        if (row.containsKey(column)) {
                            filteredRow.put(table + "_" + column, row.get(column));
                        }
                    }
                    // 添加表名标记
                    filteredRow.put("table", table);
                    allVehiclesData.add(filteredRow);
                }
            } else {
                // 如果没有选择列，则只添加标记列
                for (Map<String, Object> row : tableData) {
                    Map<String, Object> markerRow = new HashMap<>();
                    markerRow.put("vehicleId", row.get("vehicleId"));
                    if (row.containsKey("timestamp")) {
                        markerRow.put("timestamp", row.get("timestamp"));
                    }
                    markerRow.put("table", table);
                    markerRow.put(table, 1); // 标记存在
                    allVehiclesData.add(markerRow);
                }
            }
        }
        
        return allVehiclesData;
    }
}