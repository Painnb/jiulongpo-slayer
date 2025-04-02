package org.swu.vehiclecloud.controller;

import org.swu.vehiclecloud.service.ExcelService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.Resource;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/**
 * Excel导出控制器
 */
@RestController
@RequestMapping("/api/dataprocess")
@CrossOrigin(origins = "*")
public class ExcelController {

    @Autowired
    private ExcelService excelService;

    /**
     * 导出Excel文件
     * @param tableName 要导出的表名
     * @return 包含Excel文件的响应实体
     */
    @PostMapping("/business/export")
    public ResponseEntity<Resource> exportExcel(@RequestParam String tableName) {
        return excelService.exportExcel(tableName);
    }
}