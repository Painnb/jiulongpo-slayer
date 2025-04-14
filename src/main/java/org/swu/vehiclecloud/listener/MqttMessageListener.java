package org.swu.vehiclecloud.listener;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.*;
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

    // 车辆在线时间统计
    private final Map<String, Long> vehicleOnlineTime = new ConcurrentHashMap<>();
    // 为每一天定义固定模拟数据
    private final Map<String, Integer> onlineVehiclesByDay = new HashMap<>();
    private final Map<String, Integer> activeVehiclesByDay = new HashMap<>();

    // 初始化模拟数据
    {
        // 在线车辆数
        onlineVehiclesByDay.put("Mon", 120);
        onlineVehiclesByDay.put("Tue", 200);
        onlineVehiclesByDay.put("Wed", 150);
        onlineVehiclesByDay.put("Thu", 80);
        onlineVehiclesByDay.put("Fri", 70);
        onlineVehiclesByDay.put("Sat", 110);
        onlineVehiclesByDay.put("Sun", 130);

        // 活跃车辆数
        activeVehiclesByDay.put("Mon", 180);
        activeVehiclesByDay.put("Tue", 230);
        activeVehiclesByDay.put("Wed", 190);
        activeVehiclesByDay.put("Thu", 120);
        activeVehiclesByDay.put("Fri", 110);
        activeVehiclesByDay.put("Sat", 230);
        activeVehiclesByDay.put("Sun", 235);
    }

    /**
     * 处理MQTT消息事件
     */
    @EventListener
    @Transactional
    public void handleMqttMessage(MqttMessageEvent event) {
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

            // 更新车辆在线时间
            updateVehicleOnlineTime(vehicleId);

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

    /**
     * 更新车辆在线时间
     */
    private void updateVehicleOnlineTime(String vehicleId) {
        long currentTime = CHECK_INTERVAL; // 每次更新增加检测周期的时间

        // 更新车辆在线时间
        vehicleOnlineTime.put(vehicleId,
                vehicleOnlineTime.getOrDefault(vehicleId, 0L) + currentTime);

        // 模拟一些初始数据，确保有数据可以展示
        if (vehicleOnlineTime.size() < 10) {
            for (int i = 1; i <= 10; i++) {
                String vid = "vehicle" + i;
                if (!vehicleOnlineTime.containsKey(vid)) {
                    vehicleOnlineTime.put(vid, (long)(Math.random() * 10000));
                }
            }
        }
    }

    /**
     * 获取七天内的车辆活跃度统计
     */
    public Map<String, Object> getSevenDaysActivityChartData() {
        Map<String, Object> result = new HashMap<>();

        // 使用固定的xAxis数据
        List<String> xAxis = Arrays.asList("Mon", "Tue", "Wed", "Thu", "Fri", "Sat", "Sun");

        // 使用固定的示例数据
        List<Integer> onlineData = new ArrayList<>();
        List<Integer> activeData = new ArrayList<>();

        for (String day : xAxis) {
            onlineData.add(onlineVehiclesByDay.get(day));
            activeData.add(activeVehiclesByDay.get(day));
        }

        result.put("xAxis", xAxis);
        result.put("onlineData", onlineData);
        result.put("activeData", activeData);

        return result;
    }

    /**
     * 获取车辆在线时间排行
     */
    public List<Map<String, Object>> getVehicleOnlineTimeRanking(int limit) {
        List<Map<String, Object>> result = new ArrayList<>();

        // 如果没有实际数据，添加一些模拟数据
        if (vehicleOnlineTime.isEmpty()) {
            for (int i = 1; i <= 10; i++) {
                vehicleOnlineTime.put("vehicle" + i, (long)(Math.random() * 10000));
            }
        }

        // 将车辆在线时间按降序排序
        vehicleOnlineTime.entrySet().stream()
                .sorted(Map.Entry.<String, Long>comparingByValue().reversed())
                .limit(limit)
                .forEach(entry -> {
                    Map<String, Object> vehicleData = new HashMap<>();
                    vehicleData.put("vehicleId", entry.getKey());
                    vehicleData.put("onlineTime", entry.getValue());
                    result.add(vehicleData);
                });

        return result;
    }
}