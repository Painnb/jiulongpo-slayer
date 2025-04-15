package org.swu.vehiclecloud.controller;

import org.swu.vehiclecloud.annotations.PreAuthorizeRole;
import org.swu.vehiclecloud.service.QueryService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import org.swu.vehiclecloud.dto.ExcelExportRequest;
import org.swu.vehiclecloud.dto.ExcelExportAllVehiclesRequest;

import java.util.List;
import java.util.Map;

/**
 * 查询控制器
 * 提供查询异常数据的API接口
 */
@RestController
@RequestMapping("/api/query")
@CrossOrigin(origins = "*")
public class QueryController {

    @Autowired
    private QueryService queryService;

    /**
     * 查询指定表格的数据
     * @param tableName 需要查询的表格名称
     * @return 查询结果列表
     */
    @GetMapping("/business/tables/{tableName}")
    @PreAuthorizeRole(roles = {"SYS_ADMIN, BIZ_ADMIN"})
    public List<Map<String, Object>> queryTable(@PathVariable String tableName) {
        return queryService.queryTable(tableName);
    }

    /**
     * 查询指定车辆和时间范围内的组合数据
     * @param request 包含车辆ID、时间范围、选定表和列的请求对象
     * @return 查询结果列表
     */
    @PostMapping("/business/tables/combined-query")
    @PreAuthorizeRole(roles = {"USER, SYS_ADMIN, BIZ_ADMIN"})
    public List<Map<String, Object>> queryCombinedData(
            @RequestBody ExcelExportRequest request) {
        return queryService.queryCombinedData(
            request.getVehicleId(), 
            request.getStartTime(), 
            request.getEndTime(), 
            request.getSelectedTables(), 
            request.getSelectedColumns()
        );
    }
    
    /**
     * 查询时间段内所有车辆的异常数据
     * @param request 包含时间范围、选定表和列的请求对象
     * @return 查询结果列表
     */
    @PostMapping("/business/tables/all-vehicles-exceptions-query")
    @PreAuthorizeRole(roles = {"SYS_ADMIN, BIZ_ADMIN"})
    public List<Map<String, Object>> queryAllVehiclesExceptions(
            @RequestBody ExcelExportAllVehiclesRequest request) {
        return queryService.queryAllVehiclesExceptions(
            request.getStartTime(),
            request.getEndTime(),
            request.getSelectedTables(),
            request.getSelectedColumns()
        );
    }
}