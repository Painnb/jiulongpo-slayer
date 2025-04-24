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
import java.text.SimpleDateFormat;
import java.util.*;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

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
        // 步骤1: 从数据库查询指定表的所有数据
        // 使用excelMapper查询指定表的所有数据，返回结果为List<Map>结构
        // 每个Map代表一行数据，key为列名，value为对应的值
        List<Map<String, Object>> data = excelMapper.selectAllFromTable(tableName);

        // 步骤2: SQL注入防护 - 验证表名合法性
        // 使用SQLInjectionProtector工具验证表名，防止恶意输入
        // 如果验证失败，抛出IllegalArgumentException异常
        if (!SQLInjectionProtector.validateTableName(tableName)) {
            throw new IllegalArgumentException("非法的表名: " + tableName);
        }
        
        // 步骤3: 创建Excel工作簿并填充数据
        // 使用try-with-resources确保工作簿资源正确释放
        try (Workbook workbook = new XSSFWorkbook()) {
            // 创建工作表，使用表名作为工作表名称
            Sheet sheet = workbook.createSheet(tableName);
            
            // 步骤3.1: 创建表头行
            // 使用查询结果的第一行数据的键作为表头列名
            Row headerRow = sheet.createRow(0);
            if (!data.isEmpty()) {
                int cellNum = 0;
                // 遍历第一行数据的key集合，创建表头单元格
                for (String key : data.get(0).keySet()) {
                    Cell cell = headerRow.createCell(cellNum++);
                    cell.setCellValue(key);
                }
                
                // 步骤3.2: 填充数据行
                // 从第二行开始(索引1)填充实际数据
                int rowNum = 1;
                for (Map<String, Object> rowData : data) {
                    Row row = sheet.createRow(rowNum++);
                    // 每行重新初始化列索引，确保数据从第一列开始写入
                    int cellNumInRow = 0; 
                    // 遍历每行数据的value集合，填充单元格
                    for (Object value : rowData.values()) {
                        Cell cell = row.createCell(cellNumInRow++);
                        // 处理null值情况，空字符串代替null
                        if (value != null) {
                            cell.setCellValue(value.toString());
                        } else {
                            cell.setCellValue("");
                        }
                    }
                }
            }
            
            // 步骤4: 将工作簿写入字节输出流
            // 将工作簿写入字节输出流
            ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
            workbook.write(outputStream);
            
            // 步骤5: 构建HTTP响应
            // 设置响应头: Content-Type为二进制流
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_OCTET_STREAM);
            // 设置Content-Disposition为附件下载，文件名为表名.xlsx
            headers.setContentDispositionFormData("attachment", tableName + ".xlsx");
            
            // 返回包含Excel文件字节数组的响应实体
            // 返回包含Excel文件字节数组的响应实体
            return ResponseEntity.ok()
                    .headers(headers)
                    .body(new ByteArrayResource(outputStream.toByteArray()));
        } catch (Exception e) {
            // 异常处理: 捕获所有异常并包装为RuntimeException抛出
            // 包含原始异常信息以便排查问题
            throw new RuntimeException("导出Excel失败: " + e.getMessage(), e);
        }
    }


    /**
     * 导出指定车辆和时间范围内的组合数据Excel文件
     * 该方法执行以下操作：
     * 1. 验证输入参数的有效性和安全性
     * 2. 创建Excel工作簿和工作表
     * 3. 根据选择的表和列生成表头
     * 4. 查询并组合多个表的数据
     * 5. 将数据填充到Excel工作表中
     * 6. 将工作簿写入输出流并封装为HTTP响应
     * 
     * @param vehicleId 车辆ID，不能为空
     * @param startTime 开始时间，可为null
     * @param endTime 结束时间，可为null
     * @param selectedTables 选定的表名列表，不能为null或空
     * @param selectedColumns 每个表选定的列名映射，key为表名，value为列名列表
     * @return ResponseEntity<Resource> 包含以下内容：
     *         - Content-Type: application/octet-stream
     *         - Content-Disposition: attachment; filename="vehicle_data.xlsx"
     *         - 响应体为Excel文件的字节数组资源
     * @throws IllegalArgumentException 当表名验证失败时抛出
     * @throws RuntimeException 当Excel文件生成或写入失败时抛出
     */
    @Override
    public ResponseEntity<Resource> exportCombinedExcel(
            String vehicleId, 
            LocalDateTime startTime, 
            LocalDateTime endTime, 
            List<String> selectedTables,
            Map<String, List<String>> selectedColumns) {
        
        // 时间格式化器 - 用于将时间戳格式化为字符串
        // 使用标准格式"yyyy-MM-dd HH:mm:ss"，确保时间显示一致
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
        
        // 判断是否有有效的时间范围
        // 当startTime和endTime都不为null且startTime早于endTime时，hasTimeRange为true
        // 此标志决定是否在查询中使用时间范围条件
        boolean hasTimeRange = startTime != null && endTime != null && startTime.isBefore(endTime);

        // 验证表名 - 防止SQL注入攻击
        // 遍历所有选定的表名，确保每个表名都通过安全验证
        for (String table : selectedTables) {
            if (!SQLInjectionProtector.validateTableName(table)) {
                throw new IllegalArgumentException("非法的表名: " + table);
            }
        }

        // 使用try-with-resources确保工作簿资源正确释放
        try (Workbook workbook = new XSSFWorkbook()) {
            // 创建工作表，命名为"CombinedData"
            Sheet sheet = workbook.createSheet("CombinedData");
            
            // 创建表头行(第0行)
            Row headerRow = sheet.createRow(0);
            int cellNum = 0; // 列索引计数器
            
            // 固定包含vehicleId列(所有表共有的标识字段)
            headerRow.createCell(cellNum++).setCellValue("vehicleId");
            
            // 如果有时间范围才包含timestamp列
            boolean includeTimestamp = hasTimeRange;
            if (includeTimestamp) {
                // 时间戳列用于标识数据记录的时间点
                headerRow.createCell(cellNum++).setCellValue("timestamp");
            }
            
            // 创建列索引映射，用于后续数据填充时快速定位列位置
            // key: 列名(表名_列名格式)，value: 列索引
            Map<String, Integer> columnIndexMap = new HashMap<>();
            columnIndexMap.put("vehicleId", 0);  // vehicleId固定在第0列
            if (includeTimestamp) {
                columnIndexMap.put("timestamp", 1); // timestamp固定在第1列
            }
            
            // 为每个选定的表添加选择的列到表头
            // 列名格式为"表名_列名"，避免不同表的同名列冲突
            for (String table : selectedTables) {
                List<String> columns = selectedColumns.getOrDefault(table, Collections.emptyList());
                for (String column : columns) {
                    headerRow.createCell(cellNum).setCellValue(table + "_" + column);
                    columnIndexMap.put(table + "_" + column, cellNum);
                    cellNum++;
                }
            }
            
            // 添加异常表作为二进制标记(当表没有选择任何列时)
            // 这些表将作为标记列存在，值为0或1表示记录是否存在
            for (String table : selectedTables) {
                if (selectedColumns.getOrDefault(table, Collections.emptyList()).isEmpty()) {
                    headerRow.createCell(cellNum).setCellValue(table);
                    columnIndexMap.put(table, cellNum);
                    cellNum++;
                }
            }
            
            
            // 获取基础数据
            // 存储所有表的数据，每个表的数据格式为List<Map>
            List<Map<String, Object>> baseData = new ArrayList<>();
            
            // 遍历每个选定的表，根据是否有时间范围调用不同的查询方法
            for (String table : selectedTables) {
                List<Map<String, Object>> tableData;
                if (hasTimeRange) {
                    // 有时间范围的情况：从每个表获取时间戳范围内的数据
                    tableData = excelMapper.selectFromTableWithFilter(
                        table,
                        vehicleId,
                        startTime,
                        endTime
                    );
                } else {
                    // 无时间范围的情况：从每个表获取该车辆的所有记录
                    tableData = excelMapper.selectByVehicleId(
                        table,
                        vehicleId
                    );
                }
                baseData.addAll(tableData);
            }
            
            // 创建数据行，从第1行开始(第0行是表头)
            int rowNum = 1;
            SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
            
            for (Map<String, Object> baseRow : baseData) {
                Row row = sheet.createRow(rowNum++);
                
                
                // 设置vehicleId(固定在第0列)
                row.createCell(0).setCellValue(baseRow.get("vehicleId").toString());
                Object timestampObj = baseRow.get("timestamp");
                LocalDateTime timestampDate = null; // 用于存储有效的 Date 对象
                // String formattedTimestamp = ""; // 如果需要格式化的字符串
    
                if (timestampObj instanceof LocalDateTime) {
                    timestampDate = (LocalDateTime) timestampObj; // 获取有效的 Date 对象
                    // formattedTimestamp = dateFormat.format(timestampDate); // 如果后续仍需字符串格式
                } else {
                    // 处理 timestamp 不是 Date 或为 null 的情况
                    if (timestampObj != null) {
                        System.err.println("警告: 行 " + rowNum + " 的 timestamp 值不是 Date 类型，实际类型: " + timestampObj.getClass().getName() + ", 值: " + timestampObj);
                        // formattedTimestamp = timestampObj.toString(); // 可以考虑记录原始字符串值，但后续查询可能失败
                    } else {
                         System.err.println("警告: 行 " + rowNum + " 的 timestamp 值是 null");
                         // formattedTimestamp = ""; // 空字符串
                    }
                    // timestampDate 保持为 null
                }
                // 设置timestamp（如果有时间范围且timestamp不为null）
                int dataStartCol = 1; // 数据列起始索引
                if (includeTimestamp && timestampObj != null) {
                    if (timestampObj instanceof LocalDateTime) {
                        // 如果是LocalDateTime类型，使用格式化器格式化
                        row.createCell(1).setCellValue(((LocalDateTime) timestampObj).format(formatter));
                    } else {
                        // 其他类型直接转为字符串
                        row.createCell(1).setCellValue(timestampObj.toString());
                    }
                    dataStartCol = 2; // 数据列从第2列开始
                }
                
                // 填充每个选择表的数据
                for (String table : selectedTables) {
                    List<String> columns = selectedColumns.getOrDefault(table, Collections.emptyList());
                    
                    if (!columns.isEmpty()) {
                        // 获取详细数据
                        List<Map<String, Object>> tableData;
                        if (hasTimeRange) {
                            tableData = excelMapper.selectFromTableWithFilter(
                                table,
                                vehicleId,
                                timestampDate, // 直接传递 Date
                                timestampDate  // 直接传递 Date
                            );
                        } else {
                            // 无时间范围时，直接使用baseRow（假设所有表结构相同）
                            tableData = Collections.singletonList(baseRow);
                        }
                        
                        if (!tableData.isEmpty()) {
                            Map<String, Object> dataRow = tableData.get(0);
                            for (String column : columns) {
                                Object value = dataRow.get(column);
                                Integer colIndex = columnIndexMap.get(table + "_" + column);
                                if (colIndex != null && value != null) {
                                    row.createCell(colIndex).setCellValue(value.toString());
                                }
                            }
                        }
                    } else {
                        // 异常表标记
                        int exists;
                        if (hasTimeRange) {
                            exists = excelMapper.checkRecordExists(
                                table,
                                vehicleId,
                                timestampDate // 直接传递 Date
                            );
                        } else {
                            exists = excelMapper.checkRecordExistsByVehicle(
                                table,
                                vehicleId
                            );
                        }
                        Integer colIndex = columnIndexMap.get(table);
                        if (colIndex != null) {
                            row.createCell(colIndex).setCellValue(exists);
                        }
                    }
                }
            }
            
            // 自动调整列宽
            // 根据内容自动调整每列宽度，确保内容完整显示
            // 遍历所有列索引，调用autoSizeColumn方法
            for (int i = 0; i < columnIndexMap.size(); i++) {
                sheet.autoSizeColumn(i);
            }
            
            // 将工作簿写入字节输出流
            ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
            workbook.write(outputStream);
            
            // 设置HTTP响应头
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_OCTET_STREAM); // 二进制流类型
            headers.setContentDispositionFormData("attachment", "vehicle_data.xlsx"); // 附件下载
            
            // 返回包含Excel文件字节数组的响应实体
            return ResponseEntity.ok()
                    .headers(headers)
                    .body(new ByteArrayResource(outputStream.toByteArray()));
        } catch (Exception e) {
            // 异常处理: 捕获所有异常并包装为RuntimeException抛出
            // 包含原始异常信息以便排查问题
            throw new RuntimeException("导出组合Excel失败: " + e.getMessage(), e);
        }
    }
    
    /**
     * 导出时间段内所有车辆的异常数据到Excel文件
     * 该方法执行以下操作：
     * 1. 验证输入表名防止SQL注入
     * 2. 创建Excel工作簿和工作表
     * 3. 生成包含车辆ID、时间戳和选定列的表头
     * 4. 查询所有车辆和时间戳的组合数据
     * 5. 填充每辆车的异常数据到Excel工作表
     * 6. 将工作簿写入输出流并封装为HTTP响应
     * 
     * @param startTime 开始时间，不能为null
     * @param endTime 结束时间，不能为null
     * @param selectedTables 选定的表名列表，不能为null或空
     * @param selectedColumns 每个表选定的列名映射，key为表名，value为列名列表
     * @return ResponseEntity<Resource> 包含以下内容：
     *         - Content-Type: application/octet-stream
     *         - Content-Disposition: attachment; filename="all_vehicles_exceptions.xlsx"
     *         - 响应体为Excel文件的字节数组资源
     * @throws IllegalArgumentException 当表名验证失败时抛出
     * @throws RuntimeException 当Excel文件生成或写入失败时抛出
     */
    @Override
    public ResponseEntity<Resource> exportAllVehiclesExceptions(
            LocalDateTime startTime,
            LocalDateTime endTime,
            List<String> selectedTables,
            Map<String, List<String>> selectedColumns) {
            
        // 验证输入参数有效性
        if (startTime == null || endTime == null) {
            throw new IllegalArgumentException("开始时间和结束时间不能为空");
        }
        if (startTime.isAfter(endTime)) {
            throw new IllegalArgumentException("开始时间不能晚于结束时间");
        }
        if (selectedTables == null || selectedTables.isEmpty()) {
            throw new IllegalArgumentException("必须至少选择一个表");
        }
        if (selectedColumns == null) {
            throw new IllegalArgumentException("列选择映射不能为空");
        }
    
        // 验证表名 - 防止SQL注入攻击
        // 遍历所有选定的表名，确保每个表名都通过安全验证
        for (String table : selectedTables) {
            if (!SQLInjectionProtector.validateTableName(table)) {
                throw new IllegalArgumentException("非法的表名: " + table);
            }
        }
    
        try (Workbook workbook = new XSSFWorkbook()) {
            Sheet sheet = workbook.createSheet("AllVehiclesExceptions");
            
            // 创建表头行(第0行)
            Row headerRow = sheet.createRow(0);
            int cellNum = 0;
            
            // 固定包含vehicleId和timestamp列
            // 这些是所有车辆数据共有的标识字段
            headerRow.createCell(cellNum++).setCellValue("vehicleId");
            headerRow.createCell(cellNum++).setCellValue("timestamp");
            
            // 创建列索引映射，用于后续数据填充时快速定位列位置
            // key: 列名(表名_列名格式)，value: 列索引
            Map<String, Integer> columnIndexMap = new HashMap<>();
            columnIndexMap.put("vehicleId", 0);  // vehicleId固定在第0列
            columnIndexMap.put("timestamp", 1); // timestamp固定在第1列
            
            // 为每个选定的表添加选择的列到表头
            // 列名格式为"表名_列名"，避免不同表的同名列冲突
            for (String table : selectedTables) {
                List<String> columns = selectedColumns.getOrDefault(table, Collections.emptyList());
                for (String column : columns) {
                    headerRow.createCell(cellNum).setCellValue(table + "_" + column);
                    columnIndexMap.put(table + "_" + column, cellNum);
                    cellNum++;
                }
            }
            
            // 添加异常表作为二进制标记(当表没有选择任何列时)
            // 这些表将作为标记列存在，值为0或1表示记录是否存在
            for (String table : selectedTables) {
                if (selectedColumns.getOrDefault(table, Collections.emptyList()).isEmpty()) {
                    headerRow.createCell(cellNum).setCellValue(table);
                    columnIndexMap.put(table, cellNum);
                    cellNum++;
                }
            }
            
            // 1. 获取所有车辆和时间戳的组合
            // 查询在选定时间范围内，所有车辆出现的时间点
            // 返回结果格式: [{vehicleId: "id1", timestamp: "2023-01-01 12:00:00"}, ...]
            List<Map<String, Object>> timeSlots;
            try {
                timeSlots = excelMapper.selectDistinctVehicleTimeSlots(
                    selectedTables,
                    startTime,
                    endTime
                );
                if (timeSlots == null || timeSlots.isEmpty()) {
                    throw new RuntimeException("在指定时间范围内没有找到任何车辆数据");
                }
            } catch (Exception e) {
                throw new RuntimeException("查询车辆时间点失败: " + e.getMessage(), e);
            }
            
            // 2. 获取各表数据
            // 为每个选定的表查询在时间范围内的所有数据
            // 存储结构: {表名: [{列名1: 值1, 列名2: 值2, ...}, ...]}
            Map<String, List<Map<String, Object>>> tableDataMap = new HashMap<>();
            for (String table : selectedTables) {
                try {
                    List<Map<String, Object>> tableData = excelMapper.selectTableDataWithTimeRange(
                        table,
                        startTime,
                        endTime
                    );
                    if (tableData == null) {
                        throw new RuntimeException("表" + table + "查询返回null结果");
                    }
                    tableDataMap.put(table, tableData);
                } catch (Exception e) {
                    throw new RuntimeException("查询表" + table + "数据失败: " + e.getMessage(), e);
                }
            }
            
            // 3. 创建数据行
            // 从第1行开始填充数据(第0行是表头)
            int rowNum = 1;
            // 时间格式化器，用于将LocalDateTime格式化为字符串
            DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
            
            for (Map<String, Object> timeSlot : timeSlots) {
                String vehicleId = timeSlot.get("vehicleId").toString();
                LocalDateTime timestamp = (LocalDateTime) timeSlot.get("timestamp");
                
                Row excelRow = sheet.createRow(rowNum++);
                
                // 设置vehicleId和timestamp
                excelRow.createCell(0).setCellValue(vehicleId);
                excelRow.createCell(1).setCellValue(timestamp.format(formatter));
                
                // 处理每个表的数据
                for (String table : selectedTables) {
                    List<String> columns = selectedColumns.getOrDefault(table, Collections.emptyList());
                    
                    if (!columns.isEmpty()) {
                        // 查找该表在该时间点的数据
                        Optional<Map<String, Object>> tableRow = tableDataMap.get(table).stream()
                            .filter(row -> row.get("vehicleId").toString().equals(vehicleId) &&
                                         ((LocalDateTime) row.get("timestamp")).equals(timestamp))
                            .findFirst();
                        
                        // 填充选定的列
                        if (tableRow.isPresent()) {
                            Map<String, Object> dataRow = tableRow.get();
                            for (String column : columns) {
                                Object value = dataRow.get(column);
                                Integer colIndex = columnIndexMap.get(table + "_" + column);
                                if (colIndex != null && value != null) {
                                    excelRow.createCell(colIndex).setCellValue(value.toString());
                                }
                            }
                        }
                    } else {
                        // 异常表标记 - 检查该表在该时间点是否有数据
                        boolean exists = tableDataMap.get(table).stream()
                            .anyMatch(row -> row.get("vehicleId").toString().equals(vehicleId) &&
                                         ((LocalDateTime) row.get("timestamp")).equals(timestamp));
                        
                        Integer colIndex = columnIndexMap.get(table);
                        if (colIndex != null) {
                            excelRow.createCell(colIndex).setCellValue(exists ? 1 : 0);
                        }
                    }
                }
            }
            
            // 自动调整列宽
            // 根据内容自动调整每列宽度，确保内容完整显示
            // 遍历所有列索引，调用autoSizeColumn方法
            for (int i = 0; i < columnIndexMap.size(); i++) {
                sheet.autoSizeColumn(i);
            }
            
            // 将工作簿写入字节输出流
            ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
            workbook.write(outputStream);
            
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_OCTET_STREAM);
            headers.setContentDispositionFormData("attachment", "all_vehicles_exceptions.xlsx");
            
            // 返回包含Excel文件字节数组的响应实体
            return ResponseEntity.ok()
                    .headers(headers)
                    .body(new ByteArrayResource(outputStream.toByteArray()));
        } catch (IllegalArgumentException e) {
            throw e; // 直接抛出输入验证异常
        } catch (RuntimeException e) {
            throw new RuntimeException("导出所有车辆异常数据失败: " + e.getMessage() + 
                " | 参数: startTime=" + startTime + ", endTime=" + endTime + 
                ", tables=" + selectedTables, e);
        } catch (Exception e) {
            throw new RuntimeException("导出所有车辆异常数据时发生意外错误: " + e.getMessage() + 
                " | 参数: startTime=" + startTime + ", endTime=" + endTime + 
                ", tables=" + selectedTables, e);
        }
    }
}

// package org.swu.vehiclecloud.service.impl;

// import com.alibaba.excel.EasyExcel;
// import com.alibaba.excel.write.metadata.style.WriteCellStyle;
// import com.alibaba.excel.write.metadata.style.WriteFont;
// import com.alibaba.excel.write.style.HorizontalCellStyleStrategy;
// import org.apache.poi.ss.usermodel.FillPatternType;
// import org.apache.poi.ss.usermodel.IndexedColors;
// import org.springframework.beans.factory.annotation.Autowired;
// import org.springframework.core.io.ByteArrayResource;
// import org.springframework.core.io.Resource;
// import org.springframework.http.HttpHeaders;
// import org.springframework.http.MediaType;
// import org.springframework.http.ResponseEntity;
// import org.springframework.stereotype.Service;
// import org.swu.vehiclecloud.mapper.ExcelMapper;
// import org.swu.vehiclecloud.service.ExcelService;
// import org.swu.vehiclecloud.util.SQLInjectionProtector;

// import java.io.ByteArrayOutputStream;
// import java.time.LocalDateTime;
// import java.time.format.DateTimeFormatter;
// import java.util.*;
// import java.util.stream.Collectors;

// /**
//  * Excel 导出服务实现类 (EasyExcel 版本)
//  * 该类实现了 ExcelService 接口，提供将数据库表数据导出为 Excel 文件的功能
//  * 使用 Alibaba EasyExcel 库操作 Excel 文件，支持 .xlsx 格式
//  * 主要功能包括：
//  * 1. 从数据库查询指定表的数据或组合数据
//  * 2. 使用 EasyExcel 生成 Excel 文件内容
//  * 3. 将 Excel 文件作为 HTTP 响应返回
//  */
// @Service
// public class ExcelServiceImpl implements ExcelService {

//     @Autowired
//     private ExcelMapper excelMapper; // 数据库操作 Mapper

//     private static final DateTimeFormatter DATE_TIME_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

//     /**
//      * 导出指定表的 Excel 文件
//      *
//      * @param tableName 要导出的表名
//      * @return ResponseEntity 包含 Excel 文件的资源
//      * @throws RuntimeException 当导出失败时抛出
//      */
//     @Override
//     public ResponseEntity<Resource> exportExcel(String tableName) {
//         // 1. SQL 注入防护
//         if (!SQLInjectionProtector.validateTableName(tableName)) {
//             throw new IllegalArgumentException("非法的表名: " + tableName);
//         }

//         // 2. 查询数据
//         List<Map<String, Object>> data;
//         try {
//             data = excelMapper.selectAllFromTable(tableName);
//         } catch (Exception e) {
//             throw new RuntimeException("查询表数据失败: " + tableName + ", 原因: " + e.getMessage(), e);
//         }


//         // 3. 准备 EasyExcel 需要的数据格式
//         List<List<String>> head = new ArrayList<>();
//         List<List<Object>> dataList = new ArrayList<>();

//         if (data != null && !data.isEmpty()) {
//             // 3.1 创建表头 (从第一行数据的 key 获取)
//             List<String> headerKeys = new ArrayList<>(data.get(0).keySet());
//             head = headerKeys.stream().map(Collections::singletonList).collect(Collectors.toList());

//             // 3.2 转换数据格式 (List<Map<String, Object>> -> List<List<Object>>)
//             // 确保数据顺序与表头一致
//             for (Map<String, Object> rowMap : data) {
//                 List<Object> rowData = new ArrayList<>();
//                 for (String key : headerKeys) {
//                     Object value = rowMap.get(key);
//                     // 处理 LocalDateTime 格式化
//                     if (value instanceof LocalDateTime) {
//                          rowData.add(((LocalDateTime) value).format(DATE_TIME_FORMATTER));
//                     } else {
//                         rowData.add(value != null ? value.toString() : ""); // 处理 null 值
//                     }
//                 }
//                 dataList.add(rowData);
//             }
//         } else {
//              // 如果没有数据，可以创建一个空表头或返回空文件，这里创建一个空文件
//              // 或者根据业务需求抛出异常或返回特定响应
//              head.add(Collections.singletonList("无数据")); // 添加一个提示表头
//         }


//         // 4. 使用 EasyExcel 写入到内存输出流
//         ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
//         try {
//             EasyExcel.write(outputStream)
//                     .head(head) // 设置表头
//                     .sheet(tableName) // 设置工作表名称
//                     .doWrite(dataList); // 写入数据
//         } catch (Exception e) {
//             throw new RuntimeException("使用 EasyExcel 生成 Excel 文件失败: " + e.getMessage(), e);
//         }

//         // 5. 构建 HTTP 响应
//         HttpHeaders headers = new HttpHeaders();
//         headers.setContentType(MediaType.APPLICATION_OCTET_STREAM);
//         // 文件名编码，防止中文乱码 (如果需要更严格的浏览器兼容性，可以考虑 URLEncoder)
//         String encodedFileName = tableName + ".xlsx";
//         headers.setContentDispositionFormData("attachment", encodedFileName);

//         return ResponseEntity.ok()
//                 .headers(headers)
//                 .body(new ByteArrayResource(outputStream.toByteArray()));
//     }


//     /**
//      * 导出指定车辆和时间范围内的组合数据 Excel 文件
//      *
//      * @param vehicleId       车辆ID
//      * @param startTime       开始时间
//      * @param endTime         结束时间
//      * @param selectedTables  选定的表名列表
//      * @param selectedColumns 每个表选定的列名映射
//      * @return ResponseEntity 包含 Excel 文件的资源
//      * @throws RuntimeException 当导出失败时抛出
//      */
//     @Override
//     public ResponseEntity<Resource> exportCombinedExcel(
//             String vehicleId,
//             LocalDateTime startTime,
//             LocalDateTime endTime,
//             List<String> selectedTables,
//             Map<String, List<String>> selectedColumns) {

//         // 1. 验证参数和表名
//         validateInput(startTime, endTime, selectedTables, selectedColumns); // 抽取验证逻辑
//         selectedTables.forEach(this::validateTableName); // 验证每个表名

//         boolean hasTimeRange = startTime != null && endTime != null && startTime.isBefore(endTime);

//         // 2. 构建表头和列索引映射 (与原逻辑类似)
//         List<List<String>> head = new ArrayList<>();
//         Map<String, Integer> columnIndexMap = new LinkedHashMap<>(); // 使用 LinkedHashMap 保持插入顺序
//         int cellNum = 0;

//         // 固定列
//         head.add(Collections.singletonList("vehicleId"));
//         columnIndexMap.put("vehicleId", cellNum++);
//         boolean includeTimestamp = hasTimeRange; // 时间戳列只在有时间范围时添加
//         if (includeTimestamp) {
//             head.add(Collections.singletonList("timestamp"));
//             columnIndexMap.put("timestamp", cellNum++);
//         }

//         // 动态添加选定列
//         for (String table : selectedTables) {
//             List<String> columns = selectedColumns.getOrDefault(table, Collections.emptyList());
//             for (String column : columns) {
//                 String headerName = table + "_" + column;
//                 head.add(Collections.singletonList(headerName));
//                 columnIndexMap.put(headerName, cellNum++);
//             }
//         }
//         // 添加未选列的表作为标记列
//         for (String table : selectedTables) {
//              if (selectedColumns.getOrDefault(table, Collections.emptyList()).isEmpty()) {
//                  head.add(Collections.singletonList(table));
//                  columnIndexMap.put(table, cellNum++);
//              }
//         }

//         // 3. 获取基础数据 (查询逻辑保持不变)
//         // 注意：原逻辑中获取 baseData 的部分似乎是为了确定所有可能的时间点，
//         // 但在填充数据时又对每个表和时间点进行了单独查询，这可能导致性能问题。
//         // 这里假设原查询逻辑是正确的，并继续沿用。
//         // 优化建议：考虑一次性查询所有相关数据，然后在内存中进行组合。

//         // 3.1 获取所有相关的时间点（如果需要按时间点聚合）
//         //    或者获取所有相关记录（如果不需要严格按时间点聚合）
//         //    这里沿用原逻辑，获取一个基础数据集（可能包含重复的时间点，需要后续处理）
//          List<Map<String, Object>> baseData = new ArrayList<>();
//          try {
//              for (String table : selectedTables) {
//                  List<Map<String, Object>> tableData;
//                  if (hasTimeRange) {
//                      tableData = excelMapper.selectFromTableWithFilter(table, vehicleId, startTime, endTime);
//                  } else {
//                      tableData = excelMapper.selectByVehicleId(table, vehicleId);
//                  }
//                  // 添加来源表信息，便于后续处理（如果需要）
//                  if (tableData != null) {
//                     tableData.forEach(row -> row.put("__source_table__", table)); // 临时标记来源
//                     baseData.addAll(tableData);
//                  }
//              }
//          } catch (Exception e) {
//              throw new RuntimeException("查询组合数据失败: " + e.getMessage(), e);
//          }

//         // 4. 准备 EasyExcel 数据 (List<List<Object>>)
//         List<List<Object>> dataList = new ArrayList<>();
//         // 用于跟踪已处理的 vehicleId + timestamp 组合，避免重复行（如果 baseData 包含重复）
//         Set<String> processedKeys = new HashSet<>();

//         for (Map<String, Object> baseRow : baseData) {
//             String currentVehicleId = baseRow.get("vehicleId").toString();
//             LocalDateTime currentTimestamp = null;
//             Object timestampObj = baseRow.get("timestamp");

//              if (timestampObj instanceof LocalDateTime) {
//                  currentTimestamp = (LocalDateTime) timestampObj;
//              } else if (timestampObj != null) {
//                  // 尝试解析，如果格式已知且固定
//                  try {
//                      currentTimestamp = LocalDateTime.parse(timestampObj.toString(), DATE_TIME_FORMATTER);
//                  } catch (Exception e) {
//                      System.err.println("警告: 无法解析时间戳 " + timestampObj + " for vehicle " + currentVehicleId);
//                      // 可以选择跳过此行或记录错误
//                  }
//              }

//              // 构建唯一键（如果需要按时间戳聚合）
//              String uniqueKey = currentVehicleId + "_" + (includeTimestamp && currentTimestamp != null ? currentTimestamp.format(DATE_TIME_FORMATTER) : "NO_TIMESTAMP");

//              // 如果不需要按时间戳聚合，或者已经处理过这个组合，则跳过
//              if (!includeTimestamp || processedKeys.contains(uniqueKey)) {
//                  // 如果没有时间范围，我们可能只需要处理每个 vehicleId 一次，
//                  // 或者需要一种不同的聚合逻辑。当前逻辑基于时间戳。
//                  // 如果没有时间范围，可能需要调整 uniqueKey 的生成方式或跳过此检查。
//                  // 为了保持与原POI代码的逻辑（它似乎为每个 baseRow 创建一行），我们暂时注释掉这个检查
//                  // continue; // 如果需要去重，取消注释此行
//              }
//              processedKeys.add(uniqueKey); // 标记为已处理


//             List<Object> excelRowData = new ArrayList<>(Collections.nCopies(columnIndexMap.size(), "")); // 初始化为空字符串

//             // 填充固定列
//             excelRowData.set(columnIndexMap.get("vehicleId"), currentVehicleId);
//             if (includeTimestamp && currentTimestamp != null) {
//                  excelRowData.set(columnIndexMap.get("timestamp"), currentTimestamp.format(DATE_TIME_FORMATTER));
//             } else if (includeTimestamp) {
//                  excelRowData.set(columnIndexMap.get("timestamp"), ""); // 或其他占位符
//             }


//             // 填充动态列和标记列 (需要根据 vehicleId 和 timestamp 重新查询或在内存中查找)
//             // --- 这是原逻辑中比较复杂且可能低效的部分 ---
//             // 为了保持功能，我们模拟原逻辑的查询行为。
//             // 更好的方法是在步骤3中获取所有需要的数据，然后在内存中高效查找。
//             try {
//                 for (String table : selectedTables) {
//                     List<String> columns = selectedColumns.getOrDefault(table, Collections.emptyList());
//                     boolean isMarkColumnTable = columns.isEmpty();

//                     if (!isMarkColumnTable) {
//                         // 查询具体列的数据
//                         List<Map<String, Object>> detailData;
//                         if (hasTimeRange && currentTimestamp != null) {
//                              // 注意：原逻辑这里传递了两次 timestampDate，可能需要确认 mapper 实现
//                              // 假设是查询精确时间点的数据
//                             detailData = excelMapper.selectFromTableWithFilter(table, currentVehicleId, currentTimestamp, currentTimestamp);
//                         } else if (!hasTimeRange) {
//                             // 如果没有时间范围，可能需要不同的查询逻辑，或者直接使用 baseRow 中的数据（如果适用）
//                             // 假设查询该 vehicleId 的所有数据，然后找到匹配的行（如果 baseRow 不可靠）
//                             // 为了简化，我们暂时假设 baseRow 包含了当前行所需的数据（如果 __source_table__ 匹配）
//                             if(table.equals(baseRow.get("__source_table__"))) {
//                                 detailData = Collections.singletonList(baseRow);
//                             } else {
//                                 // 如果 baseRow 不是来自当前表，需要单独查询
//                                 detailData = excelMapper.selectByVehicleId(table, currentVehicleId);
//                                 // 这里需要进一步逻辑来确定使用哪条记录，原逻辑未明确
//                                 // 暂时取第一条（如果存在）
//                                 detailData = detailData.isEmpty() ? Collections.emptyList() : Collections.singletonList(detailData.get(0));
//                             }
//                         } else {
//                             detailData = Collections.emptyList(); // 时间范围存在但当前行时间戳无效
//                         }


//                         if (!detailData.isEmpty()) {
//                             Map<String, Object> dataRow = detailData.get(0); // 取第一条匹配记录
//                             for (String column : columns) {
//                                 Object value = dataRow.get(column);
//                                 Integer colIndex = columnIndexMap.get(table + "_" + column);
//                                 if (colIndex != null) {
//                                      excelRowData.set(colIndex, formatValue(value));
//                                 }
//                             }
//                         }
//                     } else {
//                         // 标记列：检查记录是否存在
//                         int exists;
//                          if (hasTimeRange && currentTimestamp != null) {
//                             exists = excelMapper.checkRecordExists(table, currentVehicleId, currentTimestamp);
//                          } else if (!hasTimeRange) {
//                             exists = excelMapper.checkRecordExistsByVehicle(table, currentVehicleId);
//                          } else {
//                             exists = 0; // 时间戳无效
//                          }
//                         Integer colIndex = columnIndexMap.get(table);
//                         if (colIndex != null) {
//                             excelRowData.set(colIndex, exists);
//                         }
//                     }
//                 }
//             } catch (Exception e) {
//                  System.err.println("处理行数据时出错: vehicleId=" + currentVehicleId + ", timestamp=" + currentTimestamp + ", error: " + e.getMessage());
//                  // 可以选择继续处理下一行或抛出异常
//                  // throw new RuntimeException("处理行数据时出错: " + e.getMessage(), e);
//             }


//             dataList.add(excelRowData);
//         }


//         // 5. 使用 EasyExcel 写入
//         ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
//         try {
//             EasyExcel.write(outputStream)
//                     .head(head)
//                     .sheet("CombinedData")
//                     // 可选：添加样式策略
//                     // .registerWriteHandler(getStyleStrategy())
//                     .doWrite(dataList);
//         } catch (Exception e) {
//             throw new RuntimeException("使用 EasyExcel 生成组合 Excel 文件失败: " + e.getMessage(), e);
//         }

//         // 6. 构建 HTTP 响应
//         HttpHeaders headers = new HttpHeaders();
//         headers.setContentType(MediaType.APPLICATION_OCTET_STREAM);
//         headers.setContentDispositionFormData("attachment", "vehicle_data.xlsx");

//         return ResponseEntity.ok()
//                 .headers(headers)
//                 .body(new ByteArrayResource(outputStream.toByteArray()));
//     }


//     /**
//      * 导出时间段内所有车辆的异常数据到 Excel 文件
//      *
//      * @param startTime       开始时间
//      * @param endTime         结束时间
//      * @param selectedTables  选定的表名列表
//      * @param selectedColumns 每个表选定的列名映射
//      * @return ResponseEntity 包含 Excel 文件的资源
//      * @throws RuntimeException 当导出失败时抛出
//      */
//     @Override
//     public ResponseEntity<Resource> exportAllVehiclesExceptions(
//             LocalDateTime startTime,
//             LocalDateTime endTime,
//             List<String> selectedTables,
//             Map<String, List<String>> selectedColumns) {

//         // 1. 验证参数和表名
//         validateInput(startTime, endTime, selectedTables, selectedColumns);
//         selectedTables.forEach(this::validateTableName);

//         // 2. 构建表头和列索引映射 (与 exportCombinedExcel 类似)
//          List<List<String>> head = new ArrayList<>();
//          Map<String, Integer> columnIndexMap = new LinkedHashMap<>();
//          int cellNum = 0;

//          head.add(Collections.singletonList("vehicleId"));
//          columnIndexMap.put("vehicleId", cellNum++);
//          head.add(Collections.singletonList("timestamp"));
//          columnIndexMap.put("timestamp", cellNum++);

//          for (String table : selectedTables) {
//              List<String> columns = selectedColumns.getOrDefault(table, Collections.emptyList());
//              for (String column : columns) {
//                  String headerName = table + "_" + column;
//                  head.add(Collections.singletonList(headerName));
//                  columnIndexMap.put(headerName, cellNum++);
//              }
//          }
//          for (String table : selectedTables) {
//               if (selectedColumns.getOrDefault(table, Collections.emptyList()).isEmpty()) {
//                   head.add(Collections.singletonList(table));
//                   columnIndexMap.put(table, cellNum++);
//               }
//          }

//         // 3. 获取所有车辆和时间戳的组合 (查询逻辑保持不变)
//         List<Map<String, Object>> timeSlots;
//         try {
//             timeSlots = excelMapper.selectDistinctVehicleTimeSlots(selectedTables, startTime, endTime);
//             if (timeSlots == null || timeSlots.isEmpty()) {
//                  // 返回一个空文件或带提示的文件，而不是抛出异常
//                  System.out.println("在指定时间范围内没有找到任何车辆数据");
//                  timeSlots = Collections.emptyList(); // 确保后续流程能处理空列表
//                  // 或者可以直接构建空响应返回
//                  // return buildEmptyExcelResponse("all_vehicles_exceptions.xlsx", "无数据");
//             }
//         } catch (Exception e) {
//             throw new RuntimeException("查询车辆时间点失败: " + e.getMessage(), e);
//         }

//         // 4. 预加载各表数据 (优化：一次性加载所需时间范围内所有表的数据)
//         Map<String, List<Map<String, Object>>> tableDataMap = new HashMap<>();
//          try {
//              for (String table : selectedTables) {
//                  List<Map<String, Object>> tableData = excelMapper.selectTableDataWithTimeRange(table, startTime, endTime);
//                  tableDataMap.put(table, tableData != null ? tableData : Collections.emptyList());
//              }
//          } catch (Exception e) {
//              throw new RuntimeException("预加载表数据失败: " + e.getMessage(), e);
//          }

//         // 5. 准备 EasyExcel 数据 (List<List<Object>>)
//         List<List<Object>> dataList = new ArrayList<>();
//         for (Map<String, Object> timeSlot : timeSlots) {
//             String vehicleId = timeSlot.get("vehicleId").toString();
//              // 确保 timestamp 是 LocalDateTime 类型
//              Object tsObj = timeSlot.get("timestamp");
//              LocalDateTime timestamp = null;
//              if (tsObj instanceof LocalDateTime) {
//                  timestamp = (LocalDateTime) tsObj;
//              } else if (tsObj != null) {
//                  try {
//                      // 尝试根据已知格式解析，如果数据库返回的是字符串或其他类型
//                      timestamp = LocalDateTime.parse(tsObj.toString(), DATE_TIME_FORMATTER);
//                  } catch (Exception e) {
//                       System.err.println("警告: 无法解析时间戳 " + tsObj + " for vehicle " + vehicleId);
//                       continue; // 跳过此时间点
//                  }
//              } else {
//                  System.err.println("警告: 时间戳为空 for vehicle " + vehicleId);
//                  continue; // 跳过此时间点
//              }

//              final LocalDateTime finalTimestamp = timestamp; // Lambda 表达式需要 final 或 effectively final

//             List<Object> excelRowData = new ArrayList<>(Collections.nCopies(columnIndexMap.size(), ""));

//             // 填充固定列
//             excelRowData.set(columnIndexMap.get("vehicleId"), vehicleId);
//             excelRowData.set(columnIndexMap.get("timestamp"), timestamp.format(DATE_TIME_FORMATTER));

//             // 填充动态列和标记列 (从预加载的数据中查找)
//             for (String table : selectedTables) {
//                 List<String> columns = selectedColumns.getOrDefault(table, Collections.emptyList());
//                 boolean isMarkColumnTable = columns.isEmpty();
//                 List<Map<String, Object>> currentTableData = tableDataMap.getOrDefault(table, Collections.emptyList());

//                 // 在预加载的数据中查找匹配的行
//                 Optional<Map<String, Object>> matchingRow = currentTableData.stream()
//                         .filter(row -> vehicleId.equals(row.get("vehicleId") != null ? row.get("vehicleId").toString() : null) &&
//                                        finalTimestamp.equals(row.get("timestamp"))) // 直接比较 LocalDateTime
//                         .findFirst();


//                 if (!isMarkColumnTable) {
//                     // 填充选定列的值
//                     if (matchingRow.isPresent()) {
//                         Map<String, Object> dataRow = matchingRow.get();
//                         for (String column : columns) {
//                             Object value = dataRow.get(column);
//                             Integer colIndex = columnIndexMap.get(table + "_" + column);
//                             if (colIndex != null) {
//                                 excelRowData.set(colIndex, formatValue(value));
//                             }
//                         }
//                     }
//                     // 如果 matchingRow 不存在，则这些列保持空字符串
//                 } else {
//                     // 标记列：检查是否存在匹配行
//                     Integer colIndex = columnIndexMap.get(table);
//                     if (colIndex != null) {
//                         excelRowData.set(colIndex, matchingRow.isPresent() ? 1 : 0);
//                     }
//                 }
//             }
//             dataList.add(excelRowData);
//         }

//         // 6. 使用 EasyExcel 写入
//         ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
//         try {
//             // 如果没有数据，写入一个带提示的空文件
//             if (dataList.isEmpty() && !head.isEmpty()) {
//                  head.clear(); // 清除之前的表头
//                  head.add(Collections.singletonList("在指定时间范围内没有找到任何车辆数据"));
//             }

//             EasyExcel.write(outputStream)
//                     .head(head)
//                     .sheet("AllVehiclesExceptions")
//                     // .registerWriteHandler(getStyleStrategy()) // 可选样式
//                     .doWrite(dataList);
//         } catch (Exception e) {
//             throw new RuntimeException("使用 EasyExcel 生成所有车辆异常 Excel 文件失败: " + e.getMessage(), e);
//         }

//         // 7. 构建 HTTP 响应
//         HttpHeaders headers = new HttpHeaders();
//         headers.setContentType(MediaType.APPLICATION_OCTET_STREAM);
//         headers.setContentDispositionFormData("attachment", "all_vehicles_exceptions.xlsx");

//         return ResponseEntity.ok()
//                 .headers(headers)
//                 .body(new ByteArrayResource(outputStream.toByteArray()));
//     }

//     // --- Helper Methods ---

//     /**
//      * 验证表名合法性
//      * @param tableName 表名
//      */
//     private void validateTableName(String tableName) {
//         if (!SQLInjectionProtector.validateTableName(tableName)) {
//             throw new IllegalArgumentException("非法的表名: " + tableName);
//         }
//     }

//     /**
//      * 验证组合导出和所有车辆异常导出的输入参数
//      */
//      private void validateInput(LocalDateTime startTime, LocalDateTime endTime, List<String> selectedTables, Map<String, List<String>> selectedColumns) {
//          // 对于 exportAllVehiclesExceptions，时间是必需的
//          // 对于 exportCombinedExcel，时间是可选的，但如果提供，则 start 必须在 end 之前
//          if (startTime != null && endTime != null && startTime.isAfter(endTime)) {
//              throw new IllegalArgumentException("开始时间不能晚于结束时间");
//          }
//          if (selectedTables == null || selectedTables.isEmpty()) {
//              throw new IllegalArgumentException("必须至少选择一个表");
//          }
//          if (selectedColumns == null) {
//              // 允许 selectedColumns 为空，表示所有表都只作为标记列
//              // throw new IllegalArgumentException("列选择映射不能为空");
//          }
//      }

//     /**
//      * 格式化单元格值，特别是处理日期和 null
//      * @param value 原始值
//      * @return 格式化后的字符串或原始对象（如果 EasyExcel 可以处理）
//      */
//     private Object formatValue(Object value) {
//         if (value == null) {
//             return "";
//         }
//         if (value instanceof LocalDateTime) {
//             return ((LocalDateTime) value).format(DATE_TIME_FORMATTER);
//         }
//         // 可以添加对其他类型的处理，如 Date, Timestamp 等
//         // if (value instanceof java.sql.Timestamp) { ... }
//         // if (value instanceof java.util.Date) { ... }

//         // 对于其他类型，让 EasyExcel 尝试自动转换
//         return value;
//     }
// }
