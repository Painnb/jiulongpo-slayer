package org.swu.vehiclecloud.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType; // Import MediaType
import org.springframework.http.codec.ServerSentEvent; // Import ServerSentEvent
import org.springframework.web.bind.annotation.*;
import org.swu.vehiclecloud.service.DataService;
import org.swu.vehiclecloud.annotations.PreAuthorizeRole;
import reactor.core.publisher.Flux; // Import Flux

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
     * 生成text/event-stream类型的响应。
     *
     * @param id SSE连接的唯一标识符。
     * @return Flux<ServerSentEvent<String>> 返回一个发射SSE事件的响应式流。
     * 该端点会返回一个持续发送数据的SSE连接，默认每秒发送一次数据。
     * 可通过/set-push-content端点动态更新推送内容。
     */
    @GetMapping(value = "/public/ssestream/{ID}", produces = MediaType.TEXT_EVENT_STREAM_VALUE) // Specify stream production
    @PreAuthorizeRole(roles = {"BIZ_ADMIN", "USER", "ADMIN"}) // Keep your custom annotation
    public Flux<ServerSentEvent<String>> streamData(@PathVariable("ID") String id) {
        // Delegate directly to the service
        return dataService.streamData(id);
    }

    /**
     * 设置推送内容 (WebFlux)
     *
     * @param id      SSE连接的唯一标识符。
     * @param content 要推送的内容(请求体中的纯文本)。
     * 设置后，所有已连接的SSE客户端将收到更新后的内容。
     * 内容会每秒推送一次，直到再次更新。
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
}