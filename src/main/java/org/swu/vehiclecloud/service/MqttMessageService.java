package org.swu.vehiclecloud.service;

import org.springframework.transaction.annotation.Transactional;
import org.swu.vehiclecloud.event.MqttMessageEvent;

@Transactional
public interface MqttMessageService {
    void subConnection();
    void handleMqttMessage(MqttMessageEvent event);
    void destroy() throws Exception;
    void Status(boolean status);
}
