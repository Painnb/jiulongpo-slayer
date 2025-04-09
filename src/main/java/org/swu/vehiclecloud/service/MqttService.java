package org.swu.vehiclecloud.service;

import org.eclipse.paho.client.mqttv3.MqttException;

import java.util.List;

public interface MqttService {
    void reinitialize(String brokerUrl, String clientId, String username, String password, List<String> topic) throws MqttException;
    void subscribeToDefaultTopics() throws MqttException;
    void connect() throws MqttException;
    void close() throws Exception;
    boolean isConnected();
}

