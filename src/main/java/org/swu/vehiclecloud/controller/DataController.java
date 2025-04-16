package org.swu.vehiclecloud.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.http.MediaType;
import org.springframework.http.codec.ServerSentEvent;
import org.springframework.web.bind.annotation.*;
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

    @GetMapping("public/exceptionpie")
    @PreAuthorizeRole(roles = {"SYS_ADMIN", "BIZ_ADMIN"})
    public ResponseEntity<List<Map<String, Object>>> getExceptionStatistics() {
        List<Map<String, Object>> statistics = dataService.getExceptionStatistics();
        return ResponseEntity.ok(statistics);
    }

    @GetMapping("public/exception-data")
    @PreAuthorizeRole(roles = {"SYS_ADMIN", "BIZ_ADMIN"})
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
    @PreAuthorizeRole(roles = {"SYS_ADMIN", "BIZ_ADMIN"})
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
    @PreAuthorizeRole(roles = {"SYS_ADMIN", "BIZ_ADMIN"})
    public ResponseEntity<List<Map<String, Object>>> getVehicleOnlineTimeRanking(
            @RequestParam(value = "limit", defaultValue = "10") int limit) {

        List<Map<String, Object>> result = new ArrayList<>();

        // 创建一些固定的示例数据
        for (int i = 1; i <= limit; i++) {
            Map<String, Object> vehicle = new HashMap<>();
            vehicle.put("vehicleId", "vehicle" + i);
            vehicle.put("onlineTime", 10000 - (i * 800)); // 降序排列
            result.add(vehicle);
        }

        return ResponseEntity.ok(result);
    }
}