package org.swu.vehiclecloud.service.impl;

import lombok.extern.slf4j.Slf4j;
import org.eclipse.paho.client.mqttv3.*;
import org.eclipse.paho.client.mqttv3.persist.MemoryPersistence;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.swu.vehiclecloud.config.MqttConfigProperties;
import org.swu.vehiclecloud.service.MqttService;
import org.swu.vehiclecloud.event.MqttMessageEvent;
import java.nio.charset.StandardCharsets;
import java.util.List;

@Service
@Slf4j
public class MqttServiceImpl implements MqttService {
    private static final Logger logger = LoggerFactory.getLogger(MqttServiceImpl.class);
    private static final int DEFAULT_QOS = 0;
    private MqttClient mqttClient;
    private MqttConnectOptions currentConnectOptions;
    private final MqttConfigProperties config;
    private final ApplicationEventPublisher  mqttEventPublisher;
    //private boolean isConnected = false;

    public MqttServiceImpl(MqttConfigProperties mqttConfigProperties, ApplicationEventPublisher  mqttEventPublisher) throws MqttException {
        this.config = mqttConfigProperties;
        this.mqttEventPublisher = mqttEventPublisher;
    }

    private void initClient() {
        try {
            if (this.mqttClient != null && this.mqttClient.isConnected()) {
                this.mqttClient.disconnect();
                this.mqttClient.close();
            }

            this.mqttClient = new MqttClient(
                    config.getBrokerUrl(),
                    config.getClientId(),
                    new MemoryPersistence()
            );

            // 初始化默认连接选项
            this.currentConnectOptions = createConnectOptions(
                    config.getUsername(),
                    config.getPassword()
            );

            // 启动连接
            setupCallbacks(); //回调函数执行先于连接
            connect();
        } catch (MqttException e) {
            logger.error("Failed to initialize MQTT client", e);
        }
    }

    public void reinitialize(String brokerUrl, String clientId, String username, String password, List<String> topic) throws MqttException {
        // 更新配置
        config.setBrokerUrl(brokerUrl);
        config.setClientId(clientId);
        config.setUsername(username);
        config.setPassword(password);
        config.setSubTopics(topic);

        // 创建新的连接选项
        this.currentConnectOptions = createConnectOptions(username, password);

        // 重新初始化客户端
        try {
            if (this.mqttClient != null && this.mqttClient.isConnected()) {
                this.mqttClient.disconnect();
                this.mqttClient.close();
            }

            this.mqttClient = new MqttClient(
                    config.getBrokerUrl(),
                    config.getClientId(),
                    new MemoryPersistence()
            );

            this.currentConnectOptions = createConnectOptions(
                    config.getUsername(),
                    config.getPassword()
            );

            setupCallbacks();
        } catch (MqttException e) {
            logger.error("Failed to initialize MQTT client", e);
        }
    }

    // 创建连接配置
    private MqttConnectOptions createConnectOptions(String username, String password) throws MqttException {
        MqttConnectOptions options = new MqttConnectOptions();
        options.setUserName(username);
        options.setPassword(password.toCharArray());
        options.setCleanSession(true);
        options.setAutomaticReconnect(true); // 启用重连机制
        options.setConnectionTimeout(10); // 设置连接超时时间为10秒

        return options;
    }

    // 自定义回调函数
    private void setupCallbacks() {
        mqttClient.setCallback(new MqttCallback() {
            @Override
            public void connectionLost(Throwable cause) {
                logger.error("MQTT connection lost", cause);
            }

            @Override
            public void messageArrived(String topic, MqttMessage message) throws Exception {
                //System.out.println("message: " + message.toString());
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
    }

    // 延时初始化
    public void connect() throws MqttException {
        if (mqttClient == null) {
            initClient();
            return;
        }

        try {
            if (!mqttClient.isConnected()) {
                mqttClient.connect(currentConnectOptions);
                logger.info("MQTT connected to {}", config.getBrokerUrl());
                subscribeToDefaultTopics();
            }
        } catch (MqttException e) {
            logger.error("连接失败: {}", e.getMessage());
            throw e; // 抛出异常让调用方处理
        }
    }

    public void subscribe(String topic, int qos) throws MqttException {
        if (!isConnected()) {
            throw new MqttException(MqttException.REASON_CODE_CLIENT_NOT_CONNECTED);
        }
        mqttClient.subscribe(topic, qos);
        log.info("Subscribed to topic: {}", topic);
    }

    // 订阅默认的主题列表
    public void subscribeToDefaultTopics() throws MqttException {
        if (config.getSubTopics() != null) {
            for (String topic : config.getSubTopics()) {
                subscribe(topic, DEFAULT_QOS);
                logger.info("Subscribed to topic: {}", topic);
            }
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

