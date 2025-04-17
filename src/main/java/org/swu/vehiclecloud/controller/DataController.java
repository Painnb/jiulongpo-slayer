package org.swu.vehiclecloud.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.http.MediaType;
import org.springframework.http.codec.ServerSentEvent;
import org.springframework.web.bind.annotation.*;
import org.swu.vehiclecloud.controller.template.ApiResult;
import org.swu.vehiclecloud.dto.AnomalyStat;
import org.swu.vehiclecloud.dto.VehicleExceptionCount;
import org.swu.vehiclecloud.service.DataService;
import org.swu.vehiclecloud.annotations.PreAuthorizeRole;
import org.swu.vehiclecloud.listener.MqttMessageListener;
import reactor.core.publisher.Flux;


import java.time.LocalDateTime;
import java.util.*;


/**
 * 数据控制器 (WebFlux版本)，提供SSE数据流相关的REST接口。
 * 使用@CrossOrigin注解允许跨域访问。
 */
@RestController
@RequestMapping("/api/datacontroller")
@CrossOrigin(origins = "*")
public class DataController {

    @Autowired
    private DataService dataService;

    @Autowired
    private MqttMessageListener mqttMessageListener;

    /**
     * 获取SSE数据流 (WebFlux)
     */
    @GetMapping(value = "/public/ssestream/{ID}", produces = MediaType.TEXT_EVENT_STREAM_VALUE) // 指定流式响应类型
    @PreAuthorizeRole(roles = {"SYS_ADMIN", "BIZ_ADMIN", "USER"})
    public Flux<ServerSentEvent<String>> streamData(@PathVariable("ID") String id) {
        return dataService.streamData(id);
    }

    /**
     * 设置推送内容 (WebFlux)
     */
    @PostMapping("/public/setpushcontent/{ID}")
    @PreAuthorizeRole(roles = {"SYS_ADMIN", "BIZ_ADMIN", "USER"})
    public void setPushContent(@PathVariable("ID") String id, @RequestBody String content) {
        dataService.setPushContent(id, content);
    }

    /**
     * 获取所有异常的数量
     */
    @GetMapping("public/exceptionpie")
    @PreAuthorizeRole(roles = {"SYS_ADMIN", "BIZ_ADMIN", "USER"})
    public ResponseEntity<List<Map<String, Object>>> getExceptionStatistics(@RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime startTime,
                                                                            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime endTime) {
        List<Map<String, Object>> statistics = dataService.getExceptionStatistics(startTime, endTime);
        return ResponseEntity.ok(statistics);
    }

    @GetMapping("public/exception-data")
    @PreAuthorizeRole(roles = {"SYS_ADMIN", "BIZ_ADMIN", "USER"})
    public ResponseEntity<List<Map<String, Object>>> getExceptionData(
            @RequestParam String tableName,
            @RequestParam(required = false) String vehicleId,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime startTime,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime endTime) {

        List<Map<String, Object>> data;
        if (vehicleId != null) {
            data = dataService.getExceptionDataWithFilter(tableName, vehicleId, startTime, endTime);
        } else {
            data = dataService.getExceptionDataWithTimeRange(tableName, startTime, endTime);
        }

        return ResponseEntity.ok(data);
    }

    /**
     * 获取七天内车辆活跃度统计 - 完全硬编码的版本
     */
    @GetMapping("public/activity/seven-days")
    @PreAuthorizeRole(roles = {"SYS_ADMIN", "BIZ_ADMIN", "USER"})
    public ResponseEntity<Map<String, Object>> getSevenDaysActivityData() {
        // 直接硬编码返回你需要的格式
        Map<String, Object> result = new HashMap<>();

        // 固定的星期几
        List<String> xAxis = Arrays.asList("Mon", "Tue", "Wed", "Thu", "Fri", "Sat", "Sun");

        // 固定的在线数据
        List<Integer> onlineData = Arrays.asList(120, 200, 150, 80, 70, 110, 130);

        // 固定的活跃数据
        List<Integer> activeData = Arrays.asList(180, 230, 190, 120, 110, 230, 235);

        result.put("xAxis", xAxis);
        result.put("onlineData", onlineData);
        result.put("activeData", activeData);

        return ResponseEntity.ok(result);
    }

    /**
     * 获取车辆在线时间排行榜 - 硬编码示例数据
     */
    @GetMapping("public/activity/online-time-ranking")
    @PreAuthorizeRole(roles = {"SYS_ADMIN", "BIZ_ADMIN", "USER"})
    public ApiResult<Map<String, Object>> getVehicleOnlineTimeRanking(@RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime startTime,
                                                                      @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime endTime) {
        return dataService.getVehicleOnlineTimeRanking(startTime, endTime);
    }

    /**
     * 获取异常数据统计
     * API路径遵循统一规范：/api/datacontroller/public/exceptiondata
     *
     * @return 按value降序排列的异常统计数据
     */
    @GetMapping(
            value = "/public/exceptiondata",
            produces = MediaType.APPLICATION_JSON_VALUE
    )
    @PreAuthorizeRole(roles = {"SYS_ADMIN", "BIZ_ADMIN", "USER"})
    public List<AnomalyStat> getExceptionPieData() {

        return dataService.getExceptionPieData();
    }


    /**
     * 获取车辆异常数量统计
     * @return 车辆异常数量统计列表
     */
    @GetMapping("/public/exceptionNumber")
    @PreAuthorizeRole(roles = {"SYS_ADMIN", "BIZ_ADMIN", "USER"})
    public List<VehicleExceptionCount> getExceptionNumber(@RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime startTime,
                                                          @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime endTime) {

        return dataService.getVehicleExceptionCounts(startTime, endTime);
    }

    /**
     * 获取机器学习异常数量统计
     * @return 机器学习检测的车辆异常数量统计列表
     */
    @GetMapping("/public/getmlexceptiondata")
    @PreAuthorizeRole(roles = {"SYS_ADMIN", "BIZ_ADMIN", "USER"})
    public ApiResult<Map<String, Object>> getMlexceptionData(@RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime startTime,
                                                             @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime endTime) {
        return dataService.getMlExceptionData(startTime, endTime);
    }
}
