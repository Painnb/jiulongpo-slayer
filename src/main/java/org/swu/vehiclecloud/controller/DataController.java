package org.swu.vehiclecloud.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.MediaType; // Import MediaType
import org.springframework.http.ResponseEntity;
import org.springframework.http.codec.ServerSentEvent; // Import ServerSentEvent
import org.springframework.web.bind.annotation.*;
import org.swu.vehiclecloud.service.DataService;
import org.swu.vehiclecloud.annotations.PreAuthorizeRole;
import reactor.core.publisher.Flux; // Import Flux

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 数据控制器 (WebFlux版本)，提供SSE数据流相关的REST接口。
 * 使用@CrossOrigin注解允许跨域访问。
 */
@RestController
@RequestMapping("/api/datacontroller")
@CrossOrigin(origins = "*") // Standard Spring WebFlux CORS annotation
public class DataController {

    @Autowired
    private DataService dataService; // Inject the WebFlux DataService

    /**
     * 获取SSE数据流 (WebFlux)
     * 生成text/event-stream类型的响应，使用Server-Sent Events协议。
     *
     * @param id SSE连接的唯一标识符，用于区分不同的数据流通道
     * @return Flux<ServerSentEvent < String>> 返回一个发射SSE事件的响应式流
     * 客户端通过连接此接口并监听事件流，实时接收推送的内容
     * 可通过/set-push-content端点动态更新推送内容
     * 需要BIZ_ADMIN、USER或ADMIN角色权限
     */
    @GetMapping(value = "/public/ssestream/{ID}", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    // Specify stream production
    @PreAuthorizeRole(roles = {"BIZ_ADMIN", "USER", "ADMIN"}) // Keep your custom annotation
    public Flux<ServerSentEvent<String>> streamData(@PathVariable("ID") String id) {
        // Delegate directly to the service
        return dataService.streamData(id);
    }

    /**
     * 设置推送内容 (WebFlux)
     * 更新指定SSE通道的推送内容，所有已连接的客户端将立即收到更新
     *
     * @param id      SSE连接的唯一标识符，必须与streamData方法中的ID一致
     * @param content 要推送的内容(请求体中的纯文本)，不能为null
     *                推送内容将立即发送给所有订阅此ID的客户端，无需等待响应
     *                需要BIZ_ADMIN、USER或ADMIN角色权限
     */
    @PostMapping("/public/setpushcontent/{ID}")
    @PreAuthorizeRole(roles = {"BIZ_ADMIN", "USER", "ADMIN"}) // Keep your custom annotation
    public void setPushContent(@PathVariable("ID") String id, @RequestBody String content) {
        // Delegate directly to the service
        dataService.setPushContent(id, content);
        // Note: In WebFlux, void methods typically return Mono<Void>.
        // Spring handles this automatically for @RestController.
        // For clarity or chaining, you could return Mono.empty():
        // return Mono.fromRunnable(() -> dataService.setPushContent(id, content));
    }

    /**
     * 获取异常统计饼图数据 (WebFlux)
     * 查询系统中各类异常的统计信息，以饼图数据结构形式返回
     *
     * @return 包含异常统计数据的响应实体，数据结构为List<Map<String, Object>>格式
     *         每个Map代表一种异常类型，包含异常名称、数量等统计信息
     *         响应状态码为200(OK)表示成功获取数据
     *         此接口为公开接口，无需特定权限即可访问
     */
    @GetMapping("public/exceptionpie")
    public ResponseEntity<List<Map<String, Object>>> getExceptionStatistics() {
        List<Map<String, Object>> statistics = dataService.getExceptionStatistics();
        return ResponseEntity.ok(statistics);
    }

    @GetMapping("public/exception-data")
    public ResponseEntity<Map<String, Object>> getExceptionData(
            @RequestParam String tableName,
            @RequestParam(required = false) String vehicleId,
            @RequestParam String startTime,
            @RequestParam String endTime) {

        // 解析时间字符串为 LocalDateTime
        DateTimeFormatter formatter = DateTimeFormatter.ISO_LOCAL_DATE_TIME;
        LocalDateTime parsedStartTime = LocalDateTime.parse(startTime, formatter);
        LocalDateTime parsedEndTime = LocalDateTime.parse(endTime, formatter);

        // 获取原始数据（List<Map<String, Object>>）
        List<Map<String, Object>> rawData;
        if (vehicleId != null) {
            rawData = dataService.getExceptionDataWithFilter(tableName, vehicleId, parsedStartTime, parsedEndTime);
        } else {
            rawData = dataService.getExceptionDataWithTimeRange(tableName, parsedStartTime, parsedEndTime);
        }

        // 构建返回的 Map（仅包含请求参数 + 原始数据）
        Map<String, Object> response = new HashMap<>();
        response.put("tableName", tableName);
        if (vehicleId != null) {
            response.put("vehicleId", vehicleId);  // 仅当 vehicleId 非空时返回
        }
        response.put("startTime", startTime);
        response.put("endTime", endTime);
        response.put("exceptionData", rawData);  // 原始数据改名为 "exceptionData"（可选）

        return ResponseEntity.ok(response);
    }



}

