package org.swu.vehiclecloud.service;

import org.springframework.http.ResponseEntity;

public interface ExcelService {
    ResponseEntity<byte[]> exportExcel(String tableName);
}