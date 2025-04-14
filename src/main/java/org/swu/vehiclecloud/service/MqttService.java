package org.swu.vehiclecloud.service;

import org.eclipse.paho.client.mqttv3.MqttException;

import java.util.List;
import java.util.Map;

public interface MqttService {
    void reinitialize(String brokerUrl, String clientId, String username, String password, List<String> topic) throws MqttException;
    void subscribeToDefaultTopics() throws MqttException;
    void connect() throws MqttException;
    void close() throws Exception;
    Map<String, Object> parsePayload(byte[] payload) throws Exception;
    boolean isConnected();
}

