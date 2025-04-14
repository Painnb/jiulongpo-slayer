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
        //String json = event.getMessage();
        //logger.info("Event received - Topic: {}, Message: {}",event.getTopic(), event.getMessage());
    }
}