package org.swu.vehiclecloud.service;

import org.eclipse.paho.client.mqttv3.MqttException;
import org.swu.vehiclecloud.config.MqttConfigProperties;

public interface MqttService {
    void subscribeToDefaultTopics() throws MqttException;
    void processMessage(String topic, byte[] payload) throws MqttException;
    void close() throws Exception;
    boolean isConnected();
}

