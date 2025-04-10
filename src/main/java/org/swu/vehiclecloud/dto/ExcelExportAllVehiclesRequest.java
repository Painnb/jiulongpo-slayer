package org.swu.vehiclecloud.dto;

import lombok.Getter;
import lombok.Setter;

import java.util.List;
import java.util.Map;
import com.fasterxml.jackson.annotation.*;
import java.time.LocalDateTime;

/**
 * 用于导出时间段内所有车辆异常数据的请求DTO
 */
@Getter
@Setter
public class ExcelExportAllVehiclesRequest {
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime startTime;
    
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime endTime;
    
    private List<String> selectedTables; // 选定的异常表
    
    private Map<String, List<String>> selectedColumns; // 每个表选定的列
}