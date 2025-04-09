package org.swu.vehiclecloud.controller;

import org.eclipse.paho.client.mqttv3.MqttException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.swu.vehiclecloud.dto.MqttRequest;
import org.swu.vehiclecloud.service.MqttService;

@RestController
@RequestMapping("/mqtt")
@CrossOrigin(origins = "*")
public class MqttController {
    private static final Logger logger = LoggerFactory.getLogger(MqttController.class);

    private final MqttService mqttService;

    public MqttController(MqttService mqttService) {
        this.mqttService = mqttService;
    }

    /**
     * 处理MQTT连接或关闭的请求。
     *
     * 该函数根据传入的`connect`参数决定是启动MQTT连接还是关闭MQTT连接。
     * 如果操作成功，返回相应的成功消息；如果操作失败，返回错误信息并记录日志。
     *
     * @param connect 布尔值，指示是否启动MQTT连接。true表示启动连接，false表示关闭连接。
     * @return ResponseEntity<String> 包含操作结果的响应实体。成功时返回HTTP状态码200和操作消息，
     *         失败时返回HTTP状态码500和错误信息。
     */
    @PostMapping("/connect")
    public ResponseEntity<String> Connection(@RequestParam boolean connect) {
        try {
            // 根据connect参数决定是启动还是关闭MQTT连接
            if (connect) {
                mqttService.connect();
                return ResponseEntity.ok("MQTT connected started");
            } else {
                mqttService.close();
                return ResponseEntity.ok("MQTT connection closed");
            }
        } catch (Exception e) {
            // 捕获异常并记录错误日志，返回错误信息
            String errorMsg = "MQTT operation failed: " + e.getMessage();
            logger.error(errorMsg, e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorMsg);
        }
    }

    /**
     * 更新MQTT配置并重新初始化MQTT客户端。
     *
     * 该函数接收一个包含MQTT配置的请求体，并尝试根据这些配置重新初始化MQTT客户端。
     * 如果重新初始化成功，返回成功的响应；如果失败，返回包含错误信息的响应。
     *
     * @param config 包含MQTT配置的请求体，包括broker URL、client ID、用户名、密码和订阅主题。
     * @return ResponseEntity<String> 返回一个响应实体，包含操作结果信息。如果成功，返回状态码200和成功消息；
     *         如果失败，返回状态码500和错误信息。
     */
    @PostMapping("/config")
    public ResponseEntity<String> updateConfig(@RequestBody MqttRequest config) {
        try {
            // 根据传入的配置重新初始化MQTT客户端
            mqttService.reinitialize(
                    config.getBrokerUrl(),
                    config.getClientId(),
                    config.getUsername(),
                    config.getPassword(),
                    config.getSubTopics()
            );

            // 返回成功响应
            return ResponseEntity.ok("MQTT client reinitialized successfully");
        } catch (MqttException e) {
            // 如果重新初始化失败，返回包含错误信息的响应
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Failed to reinitialize MQTT client: " + e.getMessage());
        }
    }
}
