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

    /**
     * 导出指定表格的Excel文件
     * @param tableName 需要导出的表格名称
     * @return 包含Excel文件的HTTP响应
     */
    @GetMapping("/business/tables/{tableName}/export")
    @PreAuthorizeRole(roles = {"BIZ_ADMIN"})
    public ResponseEntity<Resource> exportExcel(@PathVariable String tableName) {
        return excelService.exportExcel(tableName);
    }

    /**
     * 导出指定车辆和时间范围内的组合数据Excel文件
     * @param request 包含车辆ID、时间范围、选定表和列的请求对象
     * @return 包含组合数据Excel文件的HTTP响应
     */
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