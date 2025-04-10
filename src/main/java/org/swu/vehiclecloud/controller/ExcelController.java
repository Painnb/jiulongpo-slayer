package org.swu.vehiclecloud.controller;

import org.swu.vehiclecloud.annotations.PreAuthorizeRole;
import org.swu.vehiclecloud.service.ExcelService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.Resource;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.swu.vehiclecloud.dto.ExcelExportRequest;
import org.swu.vehiclecloud.dto.ExcelExportAllVehiclesRequest;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/dataprocess")
@CrossOrigin(origins = "*")
public class ExcelController {

    @Autowired
    private ExcelService excelService;

    @GetMapping("/business/tables/{tableName}/export")
    @PreAuthorizeRole(roles = {"BIZ_ADMIN"})
    public ResponseEntity<Resource> exportExcel(@PathVariable String tableName) {
        return excelService.exportExcel(tableName);
    }

    @PostMapping("/business/tables/combined-export")
    @PreAuthorizeRole(roles = {"BIZ_ADMIN"})
    public ResponseEntity<Resource> exportCombinedExcel(
            @RequestBody ExcelExportRequest request) {
        return excelService.exportCombinedExcel(
            request.getVehicleId(), 
            request.getStartTime(), 
            request.getEndTime(), 
            request.getSelectedTables(), 
            request.getSelectedColumns()
        );
    }
    
    /**
     * 导出时间段内所有车辆的异常数据
     * @param request 包含时间范围、选定表和列的请求对象
     * @return 包含Excel文件的HTTP响应
     */
    @PostMapping("/business/tables/all-vehicles-exceptions-export")
    @PreAuthorizeRole(roles = {"BIZ_ADMIN"})
    public ResponseEntity<Resource> exportAllVehiclesExceptions(
            @RequestBody ExcelExportAllVehiclesRequest request) {
        return excelService.exportAllVehiclesExceptions(
            request.getStartTime(),
            request.getEndTime(),
            request.getSelectedTables(),
            request.getSelectedColumns()
        );
    }
}