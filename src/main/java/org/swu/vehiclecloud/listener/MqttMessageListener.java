package org.swu.vehiclecloud.listener;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;
import org.swu.vehiclecloud.entity.ActivityAlert;
import org.swu.vehiclecloud.event.MqttMessageEvent;
import org.swu.vehiclecloud.mapper.ActivityAlertMapper;
import org.swu.vehiclecloud.service.DataService;

import java.time.Instant;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.Date;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Component
public class MqttMessageListener {
    private static final Logger logger = LoggerFactory.getLogger(MqttMessageListener.class);
    private static final int CHECK_INTERVAL = 10; // 10秒
    private static final double LOW_SPEED_THRESHOLD = 1.0;

    @Autowired
    private ActivityAlertMapper activityAlertMapper;

    @Autowired
    private DataService dataService;

    private final ObjectMapper objectMapper = new ObjectMapper();
    private final Map<String, Long> lastUpdateTime = new ConcurrentHashMap<>();
    private final Map<String, Double> lastVelocityGNSS = new ConcurrentHashMap<>();
    private final Map<String, Double> lastVelocityCAN = new ConcurrentHashMap<>();
    private final Map<String, Boolean> isActive = new ConcurrentHashMap<>();

    /**
     * 将 UTC 时间戳转换为东八区 Date 对象
     * @param timestamp UTC 时间戳（单位：秒）
     * @return Date 对象（东八区时间）
     */
    private Date UtcToCst(long timestamp) {
        // 创建内部变量，避免修改参数的值
        long datestamp = timestamp / 1000;

        // 将毫秒转换为秒
        datestamp %= 1000;

        // 将 UTC 时间戳转换为 Instant 对象
        Instant instant = Instant.ofEpochSecond(datestamp);

        // 将 UTC 时间戳转换为东八区（Asia/Shanghai）的 ZonedDateTime 对象
        ZonedDateTime beijingTime = instant.atZone(ZoneId.of("Asia/Shanghai"));

        // 将 ZonedDateTime 转换为 java.util.Date 并返回
        return Date.from(beijingTime.toInstant());
    }

    @EventListener
    public void handleMqttMessage(MqttMessageEvent event) {

        try {
            JsonNode jsonNode = objectMapper.readTree(event.getMessage());
            String vehicleId = jsonNode.get("vehicleId").asText();
            double velocityGNSS = jsonNode.get("velocityGNSS").asDouble();
            double velocityCAN = jsonNode.get("velocityCAN").asDouble();
            long timestamp = jsonNode.get("timestamp").asLong();

            // 更新最后更新时间
            lastUpdateTime.put(vehicleId, timestamp);
            lastVelocityGNSS.put(vehicleId, velocityGNSS);
            lastVelocityCAN.put(vehicleId, velocityCAN);

            // 更新活跃状态
            boolean isVehicleActive = velocityGNSS >= LOW_SPEED_THRESHOLD && velocityCAN >= LOW_SPEED_THRESHOLD;
            isActive.put(vehicleId, isVehicleActive);

            // 推送统计数据
            pushStatistics();

        } catch (Exception e) {
            logger.error("Error processing MQTT message", e);
        }
    }

    private void pushStatistics() {
        // 计算在线车辆数量（10秒内有数据的车辆）
        long currentTime = System.currentTimeMillis();
        int onlineCount = 0;
        int activeCount = 0;

        for (Map.Entry<String, Long> entry : lastUpdateTime.entrySet()) {
            String vehicleId = entry.getKey();
            long lastUpdate = entry.getValue();
            
            // 检查是否在线（10秒内有数据）
            if (currentTime - lastUpdate <= CHECK_INTERVAL * 1000) {
                onlineCount++;
                
                // 检查是否活跃
                if (isActive.getOrDefault(vehicleId, false)) {
                    activeCount++;
                }
            }
        }

        // 推送统计数据
        String statsMessage = String.format("{\"onlineCount\":%d,\"activeCount\":%d,\"timestamp\":\"%s\"}",
                onlineCount, activeCount, new Date());
        dataService.setPushContent("activity_alerts", statsMessage);

        // String json = event.getMessage();
//         logger.info("Event received - Topic: {}, Message: {}",
//                 event.getTopic(), event.getMessage());

    }
}