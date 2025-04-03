package org.swu.vehiclecloud.service.impl;

import lombok.extern.slf4j.Slf4j;
import org.eclipse.paho.client.mqttv3.*;
import org.eclipse.paho.client.mqttv3.persist.MemoryPersistence;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.swu.vehiclecloud.config.MqttConfigProperties;
import org.swu.vehiclecloud.service.MqttService;
import org.swu.vehiclecloud.event.MqttMessageEvent;

import java.nio.charset.StandardCharsets;

@Service
@Slf4j
public class MqttServiceImpl implements MqttService {
    private static final Logger logger = LoggerFactory.getLogger(MqttServiceImpl.class);
    private static final int DEFAULT_QOS = 0;
    private final MqttClient mqttClient;
    private final MqttConfigProperties config;

    @Autowired
    private ApplicationEventPublisher  mqttEventPublisher;

    public MqttServiceImpl(MqttConfigProperties mqttConfigProperties) throws MqttException {
        this.config = mqttConfigProperties;
        this.mqttClient = new MqttClient(
                config.getBrokerUrl(),
                config.getClientId(),
                new MemoryPersistence()
        );
        MqttConnect();
    }
    private void MqttConnect() throws MqttException {
        // 创建MqttConnectOptions对象
        MqttConnectOptions mqttConnectOptions = new MqttConnectOptions();
        mqttConnectOptions.setUserName(config.getUsername());
        mqttConnectOptions.setPassword(config.getPassword().toCharArray());
        mqttConnectOptions.setCleanSession(true);
        mqttConnectOptions.setAutomaticReconnect(true);
        mqttConnectOptions.setConnectionTimeout(10);

        // 回调函数
        mqttClient.setCallback(new MqttCallback() {
            @Override
            public void connectionLost(Throwable cause) {
                logger.error("MQTT connection lost", cause);
            }

            @Override
            public void messageArrived(String topic, MqttMessage message) throws Exception {
                String payload = new String(message.getPayload(), StandardCharsets.UTF_8);
                logger.info("Received MQTT message - Topic: {}, Payload: {}", topic, payload);

                // mqtt协议解析

                // 发布事件
                mqttEventPublisher.publishEvent(new MqttMessageEvent(this, topic, payload));
            }

            @Override
            public void deliveryComplete(IMqttDeliveryToken token) {
                logger.debug("Message delivery complete");
            }
        });

        mqttClient.connect(mqttConnectOptions);
        log.info("MQTT connected to {}", config.getBrokerUrl());
    }

    public void subscribe(String topic, int qos) throws MqttException {
        if (!isConnected()) {
            throw new MqttException(MqttException.REASON_CODE_CLIENT_NOT_CONNECTED);
        }
        mqttClient.subscribe(topic, qos);
        log.info("Subscribed to topic: {}", topic);
    }

    public void subscribeToDefaultTopics() throws MqttException {
        if (config.getSubTopics() != null) {
            for (String topic : config.getSubTopics()) {
                subscribe(topic, DEFAULT_QOS);
                logger.info("Subscribed to topic: {}", topic);
            }
        }
    }

    /*
    public void receiveMessage()  throws MqttException{
        if (!mqttClient.isConnected()) {
            throw new IllegalStateException("MQTT client is not connected");
        }

        mqttClient.setCallback(new MqttCallback() {
            @Override
            public void connectionLost(Throwable cause) {
                System.out.println("Connection lost");
            }

            @Override
            public void messageArrived(String topic, MqttMessage message) throws Exception {
                JSONObject json = new JSONObject();
                json.put("topic", topic);
                json.put("message", message.toString());
                //System.out.println("topic: " + topic);
                //System.out.println("message: " + new String(message.getPayload()));
                //processMessage(topic, message.getPayload()); // 传递原始字节数组
            }

            @Override
            public void deliveryComplete(IMqttDeliveryToken token) {
                System.out.println("deliveryComplete");
            }
        });
    }
    */

    public void processMessage(String topic, byte[] payload) throws MqttException {
        try {
            String jsonString = new String(payload, StandardCharsets.UTF_8);
            // 使用 Jackson 解析 JSON
            //ObjectMapper mapper = new ObjectMapper();
            //DataModel data = mapper.readValue(jsonString, DataModel.class);

            //logger.info("Received from {}: {}", topic, data);
            // 这里添加业务逻辑（如存储到数据库）
        } catch (Exception e) {
            logger.error("Message parsing failed", e);
        }
    }

    @Override
    public void close() throws Exception {
        if (mqttClient != null) {
            if (mqttClient.isConnected()) {
                mqttClient.disconnect();
            }
            mqttClient.close();
        }
    }

    public boolean isConnected() {
        return mqttClient != null && mqttClient.isConnected();
    }
}

