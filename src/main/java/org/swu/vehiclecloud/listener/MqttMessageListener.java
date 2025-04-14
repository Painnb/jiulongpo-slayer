package org.swu.vehiclecloud.listener;

import java.time.Instant;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.Date;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import org.swu.vehiclecloud.entity.ActivityAlert;
import org.swu.vehiclecloud.event.MqttMessageEvent;
import org.swu.vehiclecloud.mapper.ActivityAlertMapper;
import org.swu.vehiclecloud.service.DataService;

@Component
public class MqttMessageListener {
    private static final Logger logger = LoggerFactory.getLogger(MqttMessageListener.class);

    // 配置化参数（可通过@Value注入）
    private static final int CHECK_INTERVAL = 10; // 10秒检测周期
    private static final double LOW_SPEED_THRESHOLD = 1.0; // 低速阈值(m/s)
    private static final int NO_DATA_ALERT_THRESHOLD = 15; // 无数据报警阈值(秒)

    @Autowired
    private ActivityAlertMapper activityAlertMapper;

    @Autowired
    private DataService dataService;

    // 车辆状态缓存
    private final Map<String, Long> lastUpdateTime = new ConcurrentHashMap<>();
    private final Map<String, Double> lastVelocityGNSS = new ConcurrentHashMap<>();
    private final Map<String, Double> lastVelocityCAN = new ConcurrentHashMap<>();
    private final Map<String, Boolean> isActive = new ConcurrentHashMap<>();

    /**
     * 处理MQTT消息事件
     */
    @EventListener
    @Transactional
    public void handleMqttMessage(MqttMessageEvent event) {
        //String json = event.getMessage();
        //logger.info("Event received - Topic: {}, Message: {}",event.getTopic(), event.getMessage());

        try {
            Map<String, Object> payload = event.getMessage();
            if (payload == null) {
                logger.warn("Received null payload");
                return;
            }

            // 解析基础数据
            String vehicleId = String.valueOf(payload.get("vehicleId"));
            double velocityGNSS = parseDouble(payload.get("velocityGNSS"));
            double velocityCAN = parseDouble(payload.get("velocityCAN"));
            long timestamp = parseTimestamp(payload.get("timestamp"));

            // 更新车辆状态缓存
            updateVehicleStatus(vehicleId, velocityGNSS, velocityCAN, timestamp);

            // 检测并保存异常数据
            checkAndSaveAlerts(vehicleId, velocityGNSS, velocityCAN, timestamp);

            // 推送实时统计信息
            pushStatistics();

        } catch (Exception e) {
            logger.error("Error processing MQTT message: {}", e.getMessage(), e);
        }
    }

    // === 私有方法 ===

    private double parseDouble(Object obj) {
        return (obj instanceof Number) ? ((Number) obj).doubleValue() : 0.0;
    }

    private long parseTimestamp(Object obj) {
        return (obj instanceof Number) ? ((Number) obj).longValue() : System.currentTimeMillis();
    }

    private void updateVehicleStatus(String vehicleId, double velocityGNSS, double velocityCAN, long timestamp) {
        lastUpdateTime.put(vehicleId, timestamp);
        lastVelocityGNSS.put(vehicleId, velocityGNSS);
        lastVelocityCAN.put(vehicleId, velocityCAN);
        isActive.put(vehicleId, velocityGNSS >= LOW_SPEED_THRESHOLD && velocityCAN >= LOW_SPEED_THRESHOLD);
    }

    /**
     * 检测异常并持久化到数据库
     */
    private void checkAndSaveAlerts(String vehicleId, double velocityGNSS, double velocityCAN, long timestamp) {
        boolean noDataAlert = isNoDataAlert(vehicleId);
        boolean lowSpeedAlert = isLowSpeedAlert(velocityGNSS, velocityCAN);

        if (noDataAlert || lowSpeedAlert) {
            ActivityAlert alert = new ActivityAlert();
            alert.setVehicleId(vehicleId);
            alert.setNoDataAlert(noDataAlert);
            alert.setLowSpeedAlert(lowSpeedAlert);
            alert.setTimestamp(new Date(timestamp));
            alert.setAlertLevel(calculateAlertLevel(velocityGNSS, velocityCAN));

            activityAlertMapper.insert(alert);
            logger.debug("Saved alert: {}", alert);
        }
    }

    private boolean isNoDataAlert(String vehicleId) {
        Long lastUpdate = lastUpdateTime.get(vehicleId);
        return lastUpdate == null ||
                (System.currentTimeMillis() - lastUpdate) > NO_DATA_ALERT_THRESHOLD * 1000;
    }

    private boolean isLowSpeedAlert(double velocityGNSS, double velocityCAN) {
        return velocityGNSS < LOW_SPEED_THRESHOLD || velocityCAN < LOW_SPEED_THRESHOLD;
    }

    private int calculateAlertLevel(double velocityGNSS, double velocityCAN) {
        return (velocityGNSS < 0.5 || velocityCAN < 0.5) ? 2 : 1;
    }

    /**
     * 推送实时统计数据
     */
    private void pushStatistics() {
        long currentTime = System.currentTimeMillis();
        int[] counts = countActiveVehicles(currentTime);

        String statsMessage = String.format(
                "{\"onlineCount\":%d,\"activeCount\":%d,\"timestamp\":\"%s\"}",
                counts[0], counts[1], new Date()
        );

        dataService.setPushContent("activity_alerts", statsMessage);
    }

    private int[] countActiveVehicles(long currentTime) {
        int onlineCount = 0;
        int activeCount = 0;

        for (Map.Entry<String, Long> entry : lastUpdateTime.entrySet()) {
            if (currentTime - entry.getValue() <= CHECK_INTERVAL * 1000) {
                onlineCount++;
                if (isActive.getOrDefault(entry.getKey(), false)) {
                    activeCount++;
                }
            }
        }

        return new int[]{onlineCount, activeCount};
    }
}
