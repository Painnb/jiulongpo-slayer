package org.swu.vehiclecloud.controller;

import org.springframework.beans.factory.annotation.Autowired;

import org.springframework.format.annotation.DateTimeFormat;

import org.springframework.http.ResponseEntity;


import org.springframework.http.MediaType;
import org.springframework.http.codec.ServerSentEvent;

import org.springframework.web.bind.annotation.*;
import org.swu.vehiclecloud.service.DataService;
import org.swu.vehiclecloud.annotations.PreAuthorizeRole;
import reactor.core.publisher.Flux;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

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

    @GetMapping(value = "/public/ssestream/{ID}", produces = MediaType.TEXT_EVENT_STREAM_VALUE) // 指定流式响应类型
    @PreAuthorizeRole(roles = {"USER, SYS_ADMIN, BIZ_ADMIN"})
    public Flux<ServerSentEvent<String>> streamData(@PathVariable("ID") String id) {
        // 直接委托给服务层处理
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
    @PreAuthorizeRole(roles = {"USER, SYS_ADMIN, BIZ_ADMIN"})
    public void setPushContent(@PathVariable("ID") String id, @RequestBody String content) {
        // 直接委托给服务层处理
        dataService.setPushContent(id, content);
        // 注意：在WebFlux中，void方法通常返回Mono<Void>
        // Spring会自动为@RestController处理这种情况
        // 为了清晰或链式调用，可以返回Mono.empty():
        // return Mono.fromRunnable(() -> dataService.setPushContent(id, content));
    }


    @GetMapping("public/exceptionpie")
    @PreAuthorizeRole(roles = {"SYS_ADMIN, BIZ_ADMIN"})
    public ResponseEntity<List<Map<String, Object>>> getExceptionStatistics() {
        List<Map<String, Object>> statistics = dataService.getExceptionStatistics();
        return ResponseEntity.ok(statistics);
    }

    @GetMapping("public/exception-data")
    @PreAuthorizeRole(roles = {"SYS_ADMIN, BIZ_ADMIN"})
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


}

