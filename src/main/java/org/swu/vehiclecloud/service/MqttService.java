package org.swu.vehiclecloud.service;

import org.eclipse.paho.client.mqttv3.MqttException;

import java.util.List;
import java.util.Map;

import org.springframework.http.ResponseEntity;
import org.springframework.transaction.annotation.Transactional;

@Transactional
public interface MqttService {
    void initClient();
    void reinitialize(String brokerUrl, String clientId, String username, String password, List<String> topic) throws MqttException;
    void subscribeToDefaultTopics() throws MqttException;
    ResponseEntity<Map<String, Object>> connect() throws MqttException;
    ResponseEntity<Map<String, Object>> close() throws Exception;
    Map<String, Object> parsePayload(byte[] payload) throws Exception;
    boolean isConnected();
}

