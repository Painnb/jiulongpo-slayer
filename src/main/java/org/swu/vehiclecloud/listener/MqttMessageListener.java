package org.swu.vehiclecloud.listener;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;
import org.swu.vehiclecloud.event.MqttMessageEvent;

@Component
public class MqttMessageListener {
    private static final Logger logger = LoggerFactory.getLogger(MqttMessageListener.class);

    @EventListener
    public void handleMqttMessage(MqttMessageEvent event) {
        logger.info("Event received - Topic: {}, Message: {}",
                event.getTopic(), event.getMessage());

        // 根据不同的topic进行不同的处理
        if (event.getTopic().contains("temperature")) {
            handleTemperatureMessage(event);
        } else if (event.getTopic().contains("alert")) {
            handleAlertMessage(event);
        }
    }

    private void handleTemperatureMessage(MqttMessageEvent event) {
        // 处理温度数据
        logger.info("Processing temperature data: {}", event.getMessage());
    }

    private void handleAlertMessage(MqttMessageEvent event) {
        // 处理警报数据
        logger.warn("Processing alert: {}", event.getMessage());
    }
}