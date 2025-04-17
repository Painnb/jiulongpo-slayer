package org.swu.vehiclecloud.service;

import org.springframework.transaction.annotation.Transactional;

@Transactional
public interface MqttMessageService {
    void destroy() throws Exception;
}
