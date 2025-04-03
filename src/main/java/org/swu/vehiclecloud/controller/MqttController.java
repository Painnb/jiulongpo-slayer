package org.swu.vehiclecloud.controller;

import jakarta.annotation.PreDestroy;
import org.eclipse.paho.client.mqttv3.MqttException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;
import org.swu.vehiclecloud.service.MqttService;

@RestController("/mqtt")
public class MqttController {
    private static final Logger logger = LoggerFactory.getLogger(MqttController.class);

    private final MqttService mqttService;

    public MqttController(MqttService mqttService) {
        this.mqttService = mqttService;
    }

    @GetMapping("/getMsg")
    public void init() {
        try{
            mqttService.subscribeToDefaultTopics();

            logger.info("MQTT controller initialized successfully");
        }catch (MqttException e) {
            logger.error("Failed to initialize MQTT controller", e);
            throw new RuntimeException("MQTT initialization failed", e);
        }
    }

    @PreDestroy
    public void shutdown() {
        try {
            if (mqttService != null) {
                mqttService.close();
            }
            logger.info("MQTT controller shutdown completed");
        } catch (Exception e) {
            logger.error("Error during MQTT controller shutdown", e);
        }
    }
}
