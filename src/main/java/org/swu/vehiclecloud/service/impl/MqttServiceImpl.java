package org.swu.vehiclecloud.service.impl;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.eclipse.paho.client.mqttv3.*;
import org.eclipse.paho.client.mqttv3.persist.MemoryPersistence;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.swu.vehiclecloud.config.MqttConfigProperties;
import org.swu.vehiclecloud.service.MqttService;
import org.swu.vehiclecloud.event.MqttMessageEvent;

import static cn.hutool.core.convert.Convert.hexToBytes;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.regex.Pattern;

@Service
@Slf4j
public class MqttServiceImpl implements MqttService {
    private static final Logger logger = LoggerFactory.getLogger(MqttServiceImpl.class);
    private static final int DEFAULT_QOS = 0;
    private MqttAsyncClient mqttClient;
    private MqttConnectOptions currentConnectOptions;
    private final MqttConfigProperties config;
    private final ApplicationEventPublisher  mqttEventPublisher;
    private final AtomicInteger receiveCount = new AtomicInteger(0);
    private final AtomicInteger parsedCount = new AtomicInteger(0);
    private static final Pattern HEX_TOPIC_PATTERN =
            Pattern.compile("^vpub/obu/state/.*_hex$");
    private ExecutorService messageProcessor;

    public MqttServiceImpl(MqttConfigProperties mqttConfigProperties, ApplicationEventPublisher  mqttEventPublisher) throws MqttException {
        this.config = mqttConfigProperties;
        this.mqttEventPublisher = mqttEventPublisher;
    }

    public void initClient() {
        if (this.messageProcessor != null) {
            shutdownExecutor(this.messageProcessor);
        }
        // 2. 创建新的线程�?
        this.messageProcessor = createThreadPool();

        try {
            if (this.mqttClient != null && this.mqttClient.isConnected()) {
                this.mqttClient.disconnect();
                this.mqttClient.close();
            }

            this.mqttClient = new MqttAsyncClient(
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
        if (this.messageProcessor != null) {
            shutdownExecutor(this.messageProcessor);
        }
        // 2. 创建新的线程�?
        this.messageProcessor = createThreadPool();

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

            this.mqttClient = new MqttAsyncClient(
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

    // 创建新的线程�?
    private ExecutorService createThreadPool() {
        return new ThreadPoolExecutor(
                4, 8, 30, TimeUnit.SECONDS,
                new LinkedBlockingQueue<>(1000),
                new ThreadPoolExecutor.CallerRunsPolicy()
        );
    }

    private void shutdownExecutor(ExecutorService executor) {
        executor.shutdown(); // 停止接收新任�?
        try {
            if (!executor.awaitTermination(5, TimeUnit.SECONDS)) {
                List<Runnable> droppedTasks = executor.shutdownNow(); // 强制终止
                logger.warn("ThreadPool did not terminate gracefully, dropped {} tasks", droppedTasks.size());
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            logger.error("ThreadPool shutdown interrupted", e);
        }
    }

        // 创建连接配置
    private MqttConnectOptions createConnectOptions(String username, String password) throws MqttException {
        MqttConnectOptions options = new MqttConnectOptions();
        options.setUserName(username);
        options.setPassword(password.toCharArray());
        options.setCleanSession(true);
        options.setAutomaticReconnect(true); // 启用重连机制
        options.setConnectionTimeout(10); // 设置连接超时时间�?10�?

        return options;
    }

    // 自定义回调函�?
    private void setupCallbacks() {
        mqttClient.setCallback(new MqttCallback() {
            @Override
            public void connectionLost(Throwable cause) {
                logger.error("MQTT connection lost", cause);
            }

            @Override
            public void messageArrived(String topic, MqttMessage message) throws Exception {


                messageProcessor.submit(() -> {
                    receiveCount.incrementAndGet();
                    try {
                        if (HEX_TOPIC_PATTERN.matcher(topic).matches()) {
                            long start = System.nanoTime();
                            String stringPayload = new String(message.getPayload(), StandardCharsets.UTF_8);
                            byte[] payload = hexToBytes(stringPayload);
                            Map<String, Object> jsonPayload = parsePayload(payload);
                            //HexFormat hexFormat = HexFormat.of();
                            //System.out.println(hexFormat.formatHex(message.getPayload()));
                            mqttEventPublisher.publishEvent(new MqttMessageEvent(this, topic, jsonPayload));
                            parsedCount.incrementAndGet();
                            logger.trace("Parsed in {} μs", (System.nanoTime() - start) / 1000);
                        } else if(topic.equals("text/vehicle/")){
                            String payloadStr = new String(message.getPayload(), StandardCharsets.UTF_8);
                            ObjectMapper objectMapper = new ObjectMapper();
                            Map<String, Object> jsonPayload = objectMapper.readValue(
                                    payloadStr,
                                    new TypeReference<Map<String, Object>>() {}
                            );
                            System.out.println(jsonPayload);
                            mqttEventPublisher.publishEvent(new MqttMessageEvent(this, topic, jsonPayload));
                        }
                    } catch (Exception e) {
                        logger.error("Process failed: topic={}, payload={}", topic,
                                bytesToHex(message.getPayload()), e);
                    }
                });
            }

            @Override
            public void deliveryComplete(IMqttDeliveryToken token) {
                logger.debug("Message delivery complete");
            }
        });
    }

    public Map<String, Object> parsePayload(byte[] payload) throws Exception {
        if (payload == null || payload.length < 16) { // 最小长度检�?
            throw new IllegalArgumentException("Invalid data content length");
        }
        Map<String, Object> result = new LinkedHashMap<>();
        ByteBuffer buffer = ByteBuffer.wrap(payload).order(ByteOrder.BIG_ENDIAN);
        Map<String, Object> headerMap = new LinkedHashMap<>();
        // 批量读取头部固定字段 (1+4+1+1+8+1 = 16字节)
        byte[] header = new byte[16];
        buffer.get(header);
        ByteBuffer headerBuffer = ByteBuffer.wrap(header).order(ByteOrder.BIG_ENDIAN);
        headerMap.put("prefix", headerBuffer.get() & 0xFF);
        headerMap.put("dataLen", headerBuffer.getInt());
        headerMap.put("dataCategory", headerBuffer.get() & 0xFF);
        headerMap.put("ver", headerBuffer.get() & 0xFF);
        headerMap.put("timestamp", headerBuffer.getLong());
        headerMap.put("ctl", headerBuffer.get() & 0xFF);
        result.put("header", headerMap);
        // 解析数据内容
        int dataLength = (int) headerMap.get("dataLen");
        //System.out.println("dataLength: " + dataLength);
        if (dataLength > 0) {
            if (buffer.remaining() < dataLength) {
                throw new IllegalArgumentException("The data length does not match");
            }
            byte[] dataContent = new byte[dataLength];
            buffer.get(dataContent);
            parseDataContent(dataContent, result);
        }
        return result;
    }

    private void parseDataContent(byte[] dataContent, Map<String, Object> result) throws Exception {
        if (dataContent == null || dataContent.length < 8 + 8 + 8 + 2) { // 最小长度检�?
            throw new IllegalArgumentException("Invalid data content length");
        }
        ByteBuffer buffer = ByteBuffer.wrap(dataContent).order(ByteOrder.BIG_ENDIAN);
        Map<String, Object> content = new LinkedHashMap<>(24); // 预分配足够容�?
        // 1. 车辆编号 (8字节字符�?)
        byte[] vehicleIdBytes = new byte[8];
        buffer.get(vehicleIdBytes);
        content.put("vehicleId", new String(vehicleIdBytes).trim());
        // 2. 消息编号 (8字节)
        content.put("messageId", buffer.getLong());
        // 3. GNSS时间�? (8字节)
        content.put("timestampGNSS", buffer.getLong());
        // 4. GNSS速度 (2字节)
        double velocityGNSS = buffer.getShort() & 0xFFFF;
        content.put("velocityGNSS", velocityGNSS);

        // 5. 位置 (12字节)
        Map<String, Object> position = new LinkedHashMap<>(3);
        long longitudeRaw = buffer.getInt() & 0xFFFFFFFFL;
        double longitudeDeg = (longitudeRaw - 1800000000L) * 1e-7;
        position.put("longitude", longitudeDeg);
        position.put("latitude", buffer.getInt() * 1e-7 - 90);
        position.put("elevation", buffer.getInt() - 5000);
        content.put("position", position);
        // 6. 航向�? (4字节)
        content.put("heading", buffer.getInt() * 1e-4);
        // 7-21. 车辆状态数�?
        //byte[] statusBytes = new byte[1 + 4 + 2 + 2*6 + 4 + 1 + 2*3 + 2 + 1];
        byte[] statusBytes = new byte[9];
        buffer.get(statusBytes);
        ByteBuffer statusBuffer = ByteBuffer.wrap(statusBytes).order(ByteOrder.BIG_ENDIAN);

        content.put("tapPos", statusBuffer.get() & 0xFF);
        int steeringAngle = statusBuffer.getInt();
        content.put("steeringAngle", steeringAngle == -1 ? 0 : steeringAngle); // 解析值无意义
        //content.put("velocityCAN", statusBuffer.getShort());
        //content.put("accelerationLon", statusBuffer.getShort());
        //content.put("accelerationLat", statusBuffer.getShort());
        //content.put("accelerationVer", statusBuffer.getShort());
        //content.put("yawRate", statusBuffer.getShort());
        //content.put("accelPos", statusBuffer.getShort());
        //content.put("engineSpeed", statusBuffer.getShort());
        int engineTorque = statusBuffer.getInt();
        content.put("engineTorque", engineTorque == -1 ? 0 : engineTorque); // 解析值无意义
        //content.put("brakeFlag", statusBuffer.get() & 0xFF);
        //content.put("brakePos", statusBuffer.getShort());
        //content.put("brakePressure", statusBuffer.getShort());
        //content.put("fuelConsumption", statusBuffer.getShort());
        //content.put("driveMode", statusBuffer.get() & 0xFF);


        //error
        // 22. 目的地位�? (8字节)
        Map<String, Object> destLocation = new LinkedHashMap<>(2);
        int destLocationLongitude = buffer.getInt();
        destLocation.put("longitude", (destLocationLongitude == -1 ? 0 : destLocationLongitude) * 1e-7 - 180);// 解析值无意义
        int destLocationLatitude = buffer.getInt();
        destLocation.put("latitude", (destLocationLatitude == -1 ? 0 : destLocationLatitude) * 1e-7 - 90); // 解析值无意义
        content.put("destLocation", destLocation);
        // 23. 途经�?
        //int passPointsNum = buffer.get() & 0xFF;
        //content.put("passPointsNum", passPointsNum);
        int passPointsNum = buffer.get();
        content.put("passPointsNum", passPointsNum == -1 ? 0 : passPointsNum); // 解析值无意义
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

        result.put("body", content);
    }

    // 延时初始�?
    public synchronized ResponseEntity<Map<String, Object>> connect() throws MqttException {
        if (mqttClient == null) {
            initClient();
            //return ResponseEntity.of(Optional.of(Map.of("status", "error", "message", "MQTT initialization successfully")));
        }

        try {
            if (!mqttClient.isConnected()) {
                mqttClient.connect(currentConnectOptions).waitForCompletion();
                logger.info("MQTT connected to {}", config.getBrokerUrl());
                subscribeToDefaultTopics();
            }
        } catch (MqttException e) {
            logger.error("Failed to connect: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("status", "error", "message", "Failed to connect to MQTT broker"));
        }
        return ResponseEntity.ok(Map.of("status", "200", "message", "MQTT connected"));
    }

    public void subscribe(String topic, int qos) throws MqttException {
        if (!isConnected()) {
            throw new MqttException(MqttException.REASON_CODE_CLIENT_NOT_CONNECTED);
        }
        mqttClient.subscribe(topic, qos);
        log.info("Subscribed to topic: {}", topic);
    }

    // 订阅默认的主题列�?
    public void subscribeToDefaultTopics() throws MqttException {
        if (config.getSubTopics() != null) {
            for (String topic : config.getSubTopics()) {
                subscribe(topic, DEFAULT_QOS);
                logger.info("Subscribed to topic: {}", topic);
            }
        }
    }

    @Override
    public ResponseEntity<Map<String, Object>> close() throws Exception {
        try {
            shutdownExecutor(messageProcessor); // 复用关闭逻辑
        } finally {
            if (mqttClient != null) {
                try {
                    if (mqttClient.isConnected()) mqttClient.disconnect();
                    mqttClient.close();
                    logger.info("MQTT disconnected");

                    System.out.println("receiveCount: " + receiveCount.get());
                    System.out.println("parserCount: " + parsedCount.get());
                } catch (MqttException e) {
                    throw new Exception("MQTT close failed", e);
                }
            }
            return ResponseEntity.ok(Map.of("status", "200", "message", "MQTT disconnected"));
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

