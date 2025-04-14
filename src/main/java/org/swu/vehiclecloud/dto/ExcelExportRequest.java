package org.swu.vehiclecloud.dto;

import lombok.Getter;
import lombok.Setter;

import java.util.List;
import java.util.Map;
import java.util.Date;
import com.fasterxml.jackson.annotation.*;
import java.time.LocalDateTime;
/**
 * 导出单个车辆异常数据的请求DTO
 */
@Getter
@Setter
public class ExcelExportRequest {
    /**
     * 车辆ID
     */
    private String vehicleId;
    
    /**
     * 开始时间
     */
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime startTime;
    
    /**
     * 结束时间
     */
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime endTime;
    
    /**
     * 选定的异常表列表
     */
    private List<String> selectedTables;
    
    /**
     * 每个表选定的列映射
     * key为表名，value为该表选定的列名列表
     */
    private Map<String, List<String>> selectedColumns;
}