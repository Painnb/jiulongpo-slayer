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
import org.swu.vehiclecloud.util.SQLInjectionProtector;

import java.io.ByteArrayOutputStream;
import java.util.List;
import java.util.Map;

/**
 * Excel导出服务实现类
 * 该类实现了ExcelService接口，提供将数据库表数据导出为Excel文件的功能
 * 使用Apache POI库操作Excel文件，支持XSSF格式(.xlsx)
 * 主要功能包括：
 * 1. 从数据库查询指定表的数据
 * 2. 创建Excel工作簿和工作表
 * 3. 生成表头和数据行
 * 4. 将Excel文件作为HTTP响应返回
 */
@Service
public class ExcelServiceImpl implements ExcelService {

    @Autowired
    private ExcelMapper excelMapper; // 数据库操作Mapper，用于查询表数据

        /**
     * 导出指定表的Excel文件
     * 该方法执行以下操作：
     * 1. 从数据库查询指定表的所有数据
     * 2. 创建Excel工作簿和工作表
     * 3. 根据查询结果的第一行数据生成表头
     * 4. 将所有数据填充到Excel工作表中
     * 5. 将工作簿写入输出流并封装为HTTP响应
     * 
     * @param tableName 要导出的表名，不能为空
     * @return ResponseEntity<Resource> 包含以下内容：
     *         - Content-Type: application/octet-stream
     *         - Content-Disposition: attachment; filename="表名.xlsx"
     *         - 响应体为Excel文件的字节数组资源
     * @throws RuntimeException 当Excel文件生成或写入失败时抛出
     */
    @Override
    public ResponseEntity<Resource> exportExcel(String tableName) {
        List<Map<String, Object>> data = excelMapper.selectAllFromTable(tableName);

        // 添加SQL注入校验
        if (!SQLInjectionProtector.validateTableName(tableName)) {
            throw new IllegalArgumentException("非法的表名: " + tableName);
        }
        
        try (Workbook workbook = new XSSFWorkbook()) {
            Sheet sheet = workbook.createSheet(tableName);
            
            // 创建表头 - 使用查询结果的第一行数据的键作为表头列名
            Row headerRow = sheet.createRow(0);
            if (!data.isEmpty()) {
                int cellNum = 0;
                for (String key : data.get(0).keySet()) {
                    Cell cell = headerRow.createCell(cellNum++);
                    cell.setCellValue(key);
                }
                
                // 填充数据 - 遍历查询结果，将每行数据写入Excel工作表
                int rowNum = 1;
                for (Map<String, Object> rowData : data) {
                    Row row = sheet.createRow(rowNum++);
                    // 每行重新初始化列索引 - 确保每行数据从第一列开始写入
                    int cellNumInRow = 0; 
                    for (Object value : rowData.values()) {
                        Cell cell = row.createCell(cellNumInRow++);
                        if (value != null) {
                            cell.setCellValue(value.toString());
                        } else {
                            cell.setCellValue("");
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
            throw new RuntimeException("导出Excel失败: " + e.getMessage(), e); // 抛出运行时异常，包含原始异常信息
        }
    }
}