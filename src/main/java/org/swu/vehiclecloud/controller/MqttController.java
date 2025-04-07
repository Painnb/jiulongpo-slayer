package org.swu.vehiclecloud.controller;

import org.eclipse.paho.client.mqttv3.MqttException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.swu.vehiclecloud.config.MqttConfigProperties;
import org.swu.vehiclecloud.dto.MqttRequest;
import org.swu.vehiclecloud.service.MqttService;

@RestController
@RequestMapping("/mqtt")
@CrossOrigin(origins = "*")
public class MqttController {
    private static final Logger logger = LoggerFactory.getLogger(MqttController.class);

    private final MqttService mqttService;

    public MqttController(MqttService mqttService) throws MqttException {
        this.mqttService = mqttService;
    }

    @PostMapping("/connect")
    public ResponseEntity<String> Connection(@RequestParam boolean connect) throws Exception {
        if (connect) {
            mqttService.connect();
            return ResponseEntity.ok("MQTT connected successfully");
        } else {
            mqttService.close();
            return ResponseEntity.ok("MQTT connection closed");
        }
    }

    @PostMapping("/config")
    public ResponseEntity<String> updateConfig(@RequestBody MqttRequest config) {
        try {
            mqttService.reinitialize(
                    config.getBrokerUrl(),
                    config.getClientId(),
                    config.getUsername(),
                    config.getPassword(),
                    config.getSubTopics()
            );

            return ResponseEntity.ok("MQTT client reinitialized successfully");
        } catch (MqttException e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Failed to reinitialize MQTT client: " + e.getMessage());
        }

    }
}
