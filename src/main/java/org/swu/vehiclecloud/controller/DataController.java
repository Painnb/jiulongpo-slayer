package org.swu.vehiclecloud.controller;

import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;
import org.swu.vehiclecloud.service.DataService;

/**
 * 数据控制器，提供SSE数据流相关的REST接口。
 * 使用@CrossOrigin允许跨域访问。
 */
@RestController
@RequestMapping("/api/datacontroller")
@CrossOrigin(origins = "*")
public class DataController {
    
    @Autowired
    private DataService dataService;
    
    /**
     * 获取SSE数据流
     * @param id SSE连接的唯一标识符。
     * @param response HttpServletResponse对象，用于设置响应头。
     * @return SseEmitter 返回配置好的SSE发射器实例。
     * 该端点会返回一个持续发送数据的SSE连接，默认每秒发送一次数据。
     * 可通过/set-push-content端点动态更新推送内容。
     */
    @GetMapping("/public/ssestream/{ID}")
    public SseEmitter streamData(@PathVariable("ID") String id, HttpServletResponse response) {
        SseEmitter emitter = dataService.streamData(id);
        response.setHeader("ID", id);
        return emitter;
    }
    
    /**
     * 设置推送内容
     * @param id SSE连接的唯一标识符。
     * @param content 要推送的内容。
     * 设置后，所有已连接的SSE客户端将收到更新后的内容。
     * 内容会每秒推送一次，直到再次更新。
     */
    @PostMapping("/public/setpushcontent/{ID}")
    public void setPushContent(@PathVariable("ID") String id, @RequestBody String content) {
        dataService.setPushContent(id, content);
    }
}
