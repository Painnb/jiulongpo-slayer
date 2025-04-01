package org.swu.vehiclecloud.service;

import org.springframework.core.io.Resource;
import org.springframework.http.ResponseEntity;

public interface ExcelService {
    ResponseEntity<Resource> exportExcel(String tableName);
}