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

import java.util.Date;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Component
public class MqttMessageListener {
    private static final Logger logger = LoggerFactory.getLogger(MqttMessageListener.class);
    private static final int EXPECTED_VEHICLES = 50;
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
    private final Map<String, Boolean> lastNoDataAlert = new ConcurrentHashMap<>();
    private final Map<String, Boolean> lastLowSpeedAlert = new ConcurrentHashMap<>();

    @EventListener
    public void handleMqttMessage(MqttMessageEvent event) {
        try {
            JsonNode jsonNode = objectMapper.readTree(event.getMessage());
            String vehicleId = jsonNode.get("vehicleId").asText();
            double velocityGNSS = jsonNode.get("velocityGNSS").asDouble();
            double velocityCAN = jsonNode.get("velocityCAN").asDouble();

            // 更新最后更新时间
            lastUpdateTime.put(vehicleId, System.currentTimeMillis());
            lastVelocityGNSS.put(vehicleId, velocityGNSS);
            lastVelocityCAN.put(vehicleId, velocityCAN);

            // 检查低速异常
            boolean isLowSpeed = velocityGNSS < LOW_SPEED_THRESHOLD && velocityCAN < LOW_SPEED_THRESHOLD;
            if (isLowSpeed != lastLowSpeedAlert.getOrDefault(vehicleId, false)) {
                handleSpeedAlert(vehicleId, isLowSpeed);
                lastLowSpeedAlert.put(vehicleId, isLowSpeed);
            }

            // 定期检查数据更新异常
            checkDataUpdate();

        } catch (Exception e) {
            logger.error("Error processing MQTT message", e);
        }
    }

    private void checkDataUpdate() {
        long currentTime = System.currentTimeMillis();
        for (Map.Entry<String, Long> entry : lastUpdateTime.entrySet()) {
            String vehicleId = entry.getKey();
            long lastUpdate = entry.getValue();
            
            boolean isNoData = currentTime - lastUpdate > CHECK_INTERVAL * 1000;
            if (isNoData != lastNoDataAlert.getOrDefault(vehicleId, false)) {
                handleNoDataAlert(vehicleId, isNoData);
                lastNoDataAlert.put(vehicleId, isNoData);
            }
        }
    }

    private void handleSpeedAlert(String vehicleId, boolean isLowSpeed) {
        ActivityAlert alert = new ActivityAlert();
        alert.setVehicleId(vehicleId);
        alert.setLowSpeedAlert(isLowSpeed);
        alert.setNoDataAlert(lastNoDataAlert.getOrDefault(vehicleId, false));
        alert.setTimestamp(new Date());
        
        activityAlertMapper.insert(alert);
        
        // 通过SSE推送异常信息
        String alertMessage = String.format("{\"vehicleId\":\"%s\",\"lowSpeedAlert\":%d,\"noDataAlert\":%d,\"timestamp\":\"%s\"}",
                vehicleId, isLowSpeed ? 1 : 0, lastNoDataAlert.getOrDefault(vehicleId, false) ? 1 : 0, new Date());
        dataService.setPushContent("activity_alerts", alertMessage);
    }

    private void handleNoDataAlert(String vehicleId, boolean isNoData) {
        ActivityAlert alert = new ActivityAlert();
        alert.setVehicleId(vehicleId);
        alert.setNoDataAlert(isNoData);
        alert.setLowSpeedAlert(lastLowSpeedAlert.getOrDefault(vehicleId, false));
        alert.setTimestamp(new Date());
        
        activityAlertMapper.insert(alert);
        
        // 通过SSE推送异常信息
        String alertMessage = String.format("{\"vehicleId\":\"%s\",\"lowSpeedAlert\":%d,\"noDataAlert\":%d,\"timestamp\":\"%s\"}",
                vehicleId, lastLowSpeedAlert.getOrDefault(vehicleId, false) ? 1 : 0, isNoData ? 1 : 0, new Date());
        dataService.setPushContent("activity_alerts", alertMessage);
    }
}