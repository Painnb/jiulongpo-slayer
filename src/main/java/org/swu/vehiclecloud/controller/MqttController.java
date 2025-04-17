package org.swu.vehiclecloud.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import org.eclipse.paho.client.mqttv3.MqttException;
import org.eclipse.paho.client.mqttv3.MqttMessage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.parameters.P;
import org.springframework.web.bind.annotation.*;
import org.swu.vehiclecloud.controller.template.ApiResult;
import org.swu.vehiclecloud.dto.MqttRequest;
import org.swu.vehiclecloud.service.MqttMessageService;
import org.swu.vehiclecloud.service.MqttService;
import org.swu.vehiclecloud.service.impl.MqttMessageImpl;

import java.util.HashMap;
import java.util.Map;
import static cn.hutool.core.convert.Convert.hexToBytes;

@RestController
@RequestMapping("/api/mqtt")
@CrossOrigin(origins = "*")
public class MqttController {
    private static final Logger logger = LoggerFactory.getLogger(MqttController.class);

    private final MqttService mqttService;
    private final MqttMessageService mqttMessage;
    private final ObjectMapper objectMapper = new ObjectMapper();

    public MqttController(MqttService mqttService, MqttMessageService mqttMessage) {
        this.mqttService = mqttService;
        this.mqttMessage = mqttMessage;
        objectMapper.enable(SerializationFeature.INDENT_OUTPUT);
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
    public Map<String, Object> Connection(@RequestParam boolean connect) throws Exception {
        // 根据connect参数决定是启动还是关闭MQTT连接
        if (connect) {
            mqttService.initClient();
            mqttService.connect();
            Map<String, Object> response = new HashMap<>();
            response.put("status", "200");
            response.put("message", "MQTT connected");

            return response;
        } else {
            //mqttMessage.destroy();
            mqttService.close();
            Map<String, Object> response = new HashMap<>();
            response.put("status", "200");
            response.put("message", "MQTT disconnected");

            return response;
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
    public ResponseEntity<Map<String, Object>> updateConfig(@RequestBody MqttRequest config) throws MqttException {
        mqttService.reinitialize(
                config.getBrokerUrl(),
                config.getClientId(),
                config.getUsername(),
                config.getPassword(),
                config.getSubTopics()
        );

        Map<String, Object> response = new HashMap<>();
        response.put("status", "200");
        response.put("message", "MQTT config updated");
        response.put("config", config);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/analysis")
    public ResponseEntity<String> processRawData(@RequestBody String rawData) {
        try {
            byte[] byteArray = hexToBytes(rawData);
            // 2. 解析字节数据为 Map（保持顺序）
            Map<String, Object> parsedData = mqttService.parsePayload(byteArray);
            // 3. 使用 Jackson 转换为 JSON（保持字段顺序）
            String jsonResult = objectMapper.writeValueAsString(parsedData);
            // 4. 返回 JSON 响应
            return ResponseEntity.ok(jsonResult);
        } catch (Exception e) {
            // 如果重新初始化失败，返回包含错误信息的响应
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Failed to reinitialize MQTT client: " + e.getMessage());
        }
    }

    @PostMapping("subscribe")
    public ResponseEntity<String> subscribe(@RequestParam boolean subscribe) throws Exception {
        if(subscribe){
            mqttMessage.Status(true);
            mqttMessage.subConnection();
            Map<String, Object> response = new HashMap<>();
            response.put("broker", "tcp://ree116bf.ala.dedicated.aliyun.emqxcloud.cn:1883");
            response.put("username", "admin");
            response.put("password", "password");

            return ResponseEntity.ok(objectMapper.writeValueAsString(response));
        } else {
            mqttMessage.Status(false);
            mqttMessage.destroy();

            return ResponseEntity.ok("Mqtt publisher closed");
        }

    }
}
