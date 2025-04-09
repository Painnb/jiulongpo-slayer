package org.swu.vehiclecloud.controller;

import org.swu.vehiclecloud.annotations.PreAuthorizeRole;
import org.swu.vehiclecloud.service.ExcelService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.Resource;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.swu.vehiclecloud.dto.ExcelExportRequest;

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
}