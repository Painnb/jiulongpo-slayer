package org.swu.vehiclecloud.service.impl;

import org.eclipse.paho.client.mqttv3.MqttAsyncClient;
import org.eclipse.paho.client.mqttv3.MqttConnectOptions;
import org.eclipse.paho.client.mqttv3.MqttMessage;
import org.eclipse.paho.client.mqttv3.persist.MemoryPersistence;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.swu.vehiclecloud.config.MqttConfigProperties;
import org.swu.vehiclecloud.event.MqttMessageEvent;
import org.swu.vehiclecloud.service.MqttMessageService;

import java.nio.charset.StandardCharsets;

@Service
public class MqttMessageImpl implements MqttMessageService {  // 实现销毁接口
    private static final Logger logger = LoggerFactory.getLogger(MqttMessageImpl.class);
    private MqttAsyncClient msgClient;
    private final MqttConfigProperties config;
    private final String clientId = "test2";
    private final String broker = "tcp://ree116bf.ala.dedicated.aliyun.emqxcloud.cn:1883";

    public MqttMessageImpl(MqttConfigProperties config) {
        this.config = config;
        initializeConnection();  // 初始化时建立长连接
    }

    private synchronized void initializeConnection() {
        try {
            if (msgClient == null || !msgClient.isConnected()) {
                MemoryPersistence persistence = new MemoryPersistence();
                msgClient = new MqttAsyncClient(broker, clientId, persistence);

                MqttConnectOptions options = new MqttConnectOptions();
                options.setUserName("lcy");  // 从配置读取
                options.setPassword("0953".toCharArray());
                options.setAutomaticReconnect(true);  // 启用自动重连
                options.setConnectionTimeout(10);      // 连接超时（秒）
                options.setKeepAliveInterval(60);     // 合理设置心跳

                msgClient.connect(options).waitForCompletion();  // 阻塞等待连接完成
                logger.info("MQTT client connected");
            }
        } catch (Exception e) {
            logger.error("MQTT connection failed", e);
        }
    }

    @EventListener
    @Transactional
    public void handleMqttMessage(MqttMessageEvent event) {
        try {
            if (msgClient == null || !msgClient.isConnected()) {
                initializeConnection();  // 确保连接有效
            }

            String defaultTopic = event.getTopic();
            String topic = defaultTopic.replace("_hex", "");
            MqttMessage message = new MqttMessage(event.getMessageAsString().getBytes(StandardCharsets.UTF_8));
            System.out.println(message);
            message.setQos(config.getDefaultQos());
            msgClient.publish(topic, message);
            logger.info("Message published to topic: {}", topic);
        } catch (Exception e) {
            logger.error("Message publish failed", e);
        }
    }

    @Override
    public void destroy() throws Exception {
        if (msgClient != null && msgClient.isConnected()) {
            msgClient.disconnect();
            msgClient.close();
            logger.info("MQTT client disconnected");
        }
    }
}
