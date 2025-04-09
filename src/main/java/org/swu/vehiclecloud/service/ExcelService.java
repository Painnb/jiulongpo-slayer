package org.swu.vehiclecloud.service;

import org.springframework.core.io.Resource;
import org.springframework.http.ResponseEntity;

import java.util.Date;
import java.util.List;
import java.util.Map;
import java.time.LocalDateTime;

public interface ExcelService {
    ResponseEntity<Resource> exportExcel(String tableName);
    
    ResponseEntity<Resource> exportCombinedExcel(
            String vehicleId, 
            LocalDateTime startTime, 
            LocalDateTime endTime, 
            List<String> selectedTables,
            Map<String, List<String>> selectedColumns);
}