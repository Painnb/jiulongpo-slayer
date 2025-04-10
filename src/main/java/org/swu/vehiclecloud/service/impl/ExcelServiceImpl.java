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
import java.util.stream.Collectors;
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