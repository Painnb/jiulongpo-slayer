package org.swu.vehiclecloud.service;

import org.springframework.core.io.Resource;
import org.springframework.http.ResponseEntity;


import java.util.List;
import java.util.Map;
import java.time.LocalDateTime;

import org.springframework.transaction.annotation.Transactional;

@Transactional
public interface ExcelService {
    ResponseEntity<Resource> exportExcel(String tableName);
    
    ResponseEntity<Resource> exportCombinedExcel(
            String vehicleId, 
            LocalDateTime startTime, 
            LocalDateTime endTime, 
            List<String> selectedTables,
            Map<String, List<String>> selectedColumns);
            
    /**
     * 导出时间段内所有车辆的异常数据
     * @param startTime 开始时间
     * @param endTime 结束时间
     * @param selectedTables 选定的异常表
     * @param selectedColumns 每个表选定的列
     * @return 包含Excel文件的HTTP响应
     */
    ResponseEntity<Resource> exportAllVehiclesExceptions(
            LocalDateTime startTime,
            LocalDateTime endTime,
            List<String> selectedTables,
            Map<String, List<String>> selectedColumns);
}