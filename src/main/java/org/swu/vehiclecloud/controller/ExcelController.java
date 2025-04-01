package org.swu.vehiclecloud.controller;

import org.swu.vehiclecloud.service.ExcelService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/excel")
public class ExcelController {

    @Autowired
    private ExcelService excelService;

    @GetMapping("/export")
    public ResponseEntity<byte[]> exportExcel(@RequestParam String tableName) {
        return excelService.exportExcel(tableName);
    }
}