package org.swu.vehiclecloud.dto;

import lombok.Getter;
import lombok.Setter;

import java.util.List;
import java.util.Map;
import java.util.Date;
import com.fasterxml.jackson.annotation.*;
import java.time.LocalDateTime;
@Getter
@Setter
public class ExcelExportRequest {
    private String vehicleId;
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime startTime;
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime endTime;
    private List<String> selectedTables;
    private Map<String, List<String>> selectedColumns;
}