package org.swu.vehiclecloud.service.impl;

import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.swu.vehiclecloud.mapper.ExcelMapper;
import org.swu.vehiclecloud.service.ExcelService;

import java.io.ByteArrayOutputStream;
import java.util.List;
import java.util.Map;

/**
 * Excel导出服务实现类
 */
@Service
public class ExcelServiceImpl implements ExcelService {

    @Autowired
    private ExcelMapper excelMapper;

        /**
     * 导出指定表的Excel文件
     * @param tableName 要导出的表名
     * @return 包含Excel文件的响应实体
     */
    @Override
    public ResponseEntity<Resource> exportExcel(String tableName) {
        List<Map<String, Object>> data = excelMapper.selectAllFromTable(tableName);
        
        try (Workbook workbook = new XSSFWorkbook()) {
            Sheet sheet = workbook.createSheet(tableName);
            
            // 创建表头
            Row headerRow = sheet.createRow(0);
            if (!data.isEmpty()) {
                int cellNum = 0;
                for (String key : data.get(0).keySet()) {
                    Cell cell = headerRow.createCell(cellNum++);
                    cell.setCellValue(key);
                }
                
                // 填充数据
                int rowNum = 1;
                for (Map<String, Object> rowData : data) {
                    Row row = sheet.createRow(rowNum++);
                    for (Object value : rowData.values()) {
                        Cell cell = row.createCell(cellNum++);
                        if (value != null) {
                            cell.setCellValue(value.toString());
                        }
                    }
                }
            }
            
            ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
            workbook.write(outputStream);
            
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_OCTET_STREAM);
            headers.setContentDispositionFormData("attachment", tableName + ".xlsx");
            
            return ResponseEntity.ok()
                    .headers(headers)
                    .body(new ByteArrayResource(outputStream.toByteArray()));
        } catch (Exception e) {
            throw new RuntimeException("导出Excel失败", e);
        }
    }
}