package org.swu.vehiclecloud.service.impl;

import com.fasterxml.jackson.databind.ObjectMapper;
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
import java.io.FileWriter;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.time.ZoneId;
import java.util.*;

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

                //System.out.println("payload_hex：" + bytesToHex(message.getPayload()));
                //logger.info("Received MQTT message - Topic: {}, Payload: {}", topic, payload);

                if(topic.matches("^vpub/obu/state/.*_hex$")){
                    //Map<String, Object> jsonPayload = parsePayload(message.getPayload());
                    //System.out.println("jsonPayload: " + jsonPayload);
                    mqttEventPublisher.publishEvent(new MqttMessageEvent(this, topic, parsePayload(message.getPayload())));
                } else if(topic.matches("^vpub/obu/state/[^/]+$")){
/*
                    String payload = new String(message.getPayload(), StandardCharsets.UTF_8);
                    System.out.println("Original Message: " + payload);

                    ObjectMapper mapper = new ObjectMapper();
                    Map<String, Object> payloadMap = mapper.readValue(payload, Map.class);
                    mqttEventPublisher.publishEvent(new MqttMessageEvent(this, topic, payloadMap));

                    // 写入本地文件
                    try (FileWriter writer = new FileWriter("mqtt_messages.txt", true)) {
                        //writer.write("[" + Instant.now().atZone(ZoneId.systemDefault()) + "] ");
                        //writer.write("Topic: " + topic + " | ");
                        writer.write( payload + "\n");
                    } catch (IOException e) {
                        logger.error("Failed to write message to file", e);
                    }
                     */
                }

                // mqtt协议解析

                // 发布事件
            }

            @Override
            public void deliveryComplete(IMqttDeliveryToken token) {
                logger.debug("Message delivery complete");
            }
        });
    }

    private Map<String, Object> parsePayload(byte[] payload) throws Exception {
        if (payload == null || payload.length < 16) { // 最小长度检查
            throw new IllegalArgumentException("无效的数据长度");
        }
        Map<String, Object> result = new LinkedHashMap<>();
        ByteBuffer buffer = ByteBuffer.wrap(payload).order(ByteOrder.BIG_ENDIAN);
        // 批量读取头部固定字段 (1+4+1+1+8+1 = 16字节)
        byte[] header = new byte[16];
        buffer.get(header);
        ByteBuffer headerBuffer = ByteBuffer.wrap(header).order(ByteOrder.BIG_ENDIAN);
        result.put("prefix", headerBuffer.get() & 0xFF);
        result.put("dataLen", headerBuffer.getInt());
        result.put("dataCategory", headerBuffer.get() & 0xFF);
        result.put("ver", headerBuffer.get() & 0xFF);
        result.put("timestamp", headerBuffer.getLong());
        result.put("ctl", headerBuffer.get() & 0xFF);
        // 解析数据内容
        int dataLength = (int) result.get("dataLen");
        System.out.println("dataLength: " + dataLength);
        if (dataLength > 0) {
            if (buffer.remaining() < dataLength) {
                throw new IllegalArgumentException("数据长度不匹配");
            }
            byte[] dataContent = new byte[dataLength];
            buffer.get(dataContent);
            parseDataContent(dataContent, result);
        }
        return result;
    }

    private void parseDataContent(byte[] dataContent, Map<String, Object> result) throws Exception {
        if (dataContent == null || dataContent.length < 8 + 8 + 8 + 2) { // 最小长度检查
            throw new IllegalArgumentException("无效的数据内容长度");
        }
        ByteBuffer buffer = ByteBuffer.wrap(dataContent).order(ByteOrder.BIG_ENDIAN);
        Map<String, Object> content = new LinkedHashMap<>(24); // 预分配足够容量
        // 1. 车辆编号 (8字节字符串)
        byte[] vehicleIdBytes = new byte[8];
        buffer.get(vehicleIdBytes);
        content.put("vehicleId", new String(vehicleIdBytes).trim());
        // 2. 消息编号 (8字节)
        content.put("messageId", buffer.getLong());
        // 3. GNSS时间戳 (8字节)
        content.put("timestampGNSS", buffer.getLong());
        // 4. GNSS速度 (2字节)
        content.put("velocityGNSS", buffer.getShort() & 0xFF);

        // 5. 位置 (12字节)
        Map<String, Object> position = new LinkedHashMap<>(3);
        long longitudeRaw = buffer.getInt() & 0xFFFFFFFFL;
        double longitudeDeg = (longitudeRaw - 1800000000L) * 1e-7;
        position.put("longitude", longitudeDeg);
        position.put("latitude", buffer.getInt() * 1e-7 - 90);
        position.put("elevation", buffer.getInt() - 5000);
        content.put("position", position);
        // 6. 航向角 (4字节)
        content.put("heading", buffer.getInt() * 1e-4);
        // 7-21. 车辆状态数据
        //byte[] statusBytes = new byte[1 + 4 + 2 + 2*6 + 4 + 1 + 2*3 + 2 + 1];
        byte[] statusBytes = new byte[9];
        buffer.get(statusBytes);
        ByteBuffer statusBuffer = ByteBuffer.wrap(statusBytes).order(ByteOrder.BIG_ENDIAN);

        content.put("tapPos", statusBuffer.get() & 0xFF);
        content.put("steeringAngle", statusBuffer.getInt());
        //content.put("velocityCAN", statusBuffer.getShort());
        //content.put("accelerationLon", statusBuffer.getShort());
        //content.put("accelerationLat", statusBuffer.getShort());
        //content.put("accelerationVer", statusBuffer.getShort());
        //content.put("yawRate", statusBuffer.getShort());
        //content.put("accelPos", statusBuffer.getShort());
        //content.put("engineSpeed", statusBuffer.getShort());
        content.put("engineTorque", statusBuffer.getInt());
        //content.put("brakeFlag", statusBuffer.get() & 0xFF);
        //content.put("brakePos", statusBuffer.getShort());
        //content.put("brakePressure", statusBuffer.getShort());
        //content.put("fuelConsumption", statusBuffer.getShort());
        //content.put("driveMode", statusBuffer.get() & 0xFF);


        //error
        // 22. 目的地位置 (8字节)
        Map<String, Object> destLocation = new LinkedHashMap<>(2);
        destLocation.put("longitude", buffer.getInt() * 1e-7 - 180);
        destLocation.put("latitude", buffer.getInt() * 1e-7 - 90);
        content.put("destLocation", destLocation);
        // 23. 途经点
        //int passPointsNum = buffer.get() & 0xFF;
        //content.put("passPointsNum", passPointsNum);
        content.put("passPointsNum", buffer.get());
        /*
        if (passPointsNum > 0) {
            if (buffer.remaining() < passPointsNum * 8) {
                System.out.println("途经点数据不完整");
                throw new IllegalArgumentException("途经点数据不完整");
            }

            List<Map<String, Object>> passPoints = new ArrayList<>(passPointsNum);
            for (int i = 0; i < passPointsNum; i++) {
                Map<String, Object> point = new LinkedHashMap<>(2);
                point.put("longitude", buffer.getInt());
                point.put("latitude", buffer.getInt());
                passPoints.add(point);
            }
            content.put("passPoints", passPoints);
        }
         */

        result.put("dataContent", content);
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

    private static String bytesToHex(byte[] bytes) {
        StringBuilder sb = new StringBuilder();
        for (byte b : bytes) {
            sb.append(String.format("%02x", b));
        }
        return sb.toString();
    }
}

