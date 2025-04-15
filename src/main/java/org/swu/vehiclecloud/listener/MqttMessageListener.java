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

import javax.annotation.PostConstruct;

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
     * 系统启动时初始化一些测试数据
     */
    @PostConstruct
    public void init() {
        logger.info("初始化MqttMessageListener，添加一些测试车辆数据");
        // 添加一些初始模拟数据，确保系统启动时有数据
        for (int i = 1; i <= 10; i++) {
            String vehicleId = "vehicle" + i;
            long timestamp = System.currentTimeMillis();
            double velocity = 5.0 + Math.random() * 10.0;  // 随机速度，确保大于低速阈值

            // 更新车辆状态缓存
            lastUpdateTime.put(vehicleId, timestamp);
            lastVelocityGNSS.put(vehicleId, velocity);
            isActive.put(vehicleId, velocity >= LOW_SPEED_THRESHOLD);

            // 更新车辆在线时间
            vehicleOnlineTime.put(vehicleId, (long)(Math.random() * 10000));
        }

        logger.info("初始化完成，当前缓存车辆数: {}", lastUpdateTime.size());

        // 立即推送一次统计数据
        pushStatistics();
    }

    /**
     * 处理MQTT消息事件
     */
    @EventListener
    @Transactional
    public void handleMqttMessage(MqttMessageEvent event) {
        try {
            logger.debug("接收到MQTT消息事件");
            Map<String, Object> payload = event.getMessage();
            if (payload == null) {
                logger.warn("接收到空消息载荷");
                return;
            }

            // 解析基础数据
            String vehicleId = String.valueOf(payload.get("vehicleId"));
            // 确保vehicleId不为null或空字符串
            if (vehicleId == null || vehicleId.trim().isEmpty() || vehicleId.equals("null")) {
                logger.warn("接收到无效的车辆ID");
                return;
            }

            double velocityGNSS = parseDouble(payload.get("velocityGNSS"));
            long timestamp = parseTimestamp(payload.get("timestamp"));

            logger.debug("解析消息: 车辆ID={}, GNSS速度={}, 时间戳={}",
                    vehicleId, velocityGNSS, new Date(timestamp));

            // 更新车辆状态缓存
            updateVehicleStatus(vehicleId, velocityGNSS, timestamp);

            // 检测并保存异常数据
            checkAndSaveAlerts(vehicleId, velocityGNSS, timestamp);

            // 更新车辆在线时间
            updateVehicleOnlineTime(vehicleId);

            // 推送实时统计信息
            pushStatistics();

        } catch (Exception e) {
            logger.error("处理MQTT消息时出错: {}", e.getMessage(), e);
        }
    }

    // === 私有方法 ===

    private double parseDouble(Object obj) {
        if (obj == null) {
            logger.warn("解析空对象为double值");
            return 0.0;
        }
        double result = (obj instanceof Number) ? ((Number) obj).doubleValue() : 0.0;
        logger.debug("解析对象 {} 为double值: {}", obj, result);
        return result;
    }

    private long parseTimestamp(Object obj) {
        if (obj == null) {
            logger.warn("解析空对象为时间戳");
            return System.currentTimeMillis();
        }
        long result = (obj instanceof Number) ? ((Number) obj).longValue() : System.currentTimeMillis();
        logger.debug("解析对象 {} 为时间戳: {}", obj, new Date(result));
        return result;
    }

    private void updateVehicleStatus(String vehicleId, double velocityGNSS, long timestamp) {
        logger.debug("更新车辆状态: ID={}, GNSS速度={}, 时间戳={}",
                vehicleId, velocityGNSS, new Date(timestamp));

        lastUpdateTime.put(vehicleId, timestamp);
        lastVelocityGNSS.put(vehicleId, velocityGNSS);
        boolean active = velocityGNSS >= LOW_SPEED_THRESHOLD;
        isActive.put(vehicleId, active);

        logger.debug("车辆 {} 活跃状态: {}", vehicleId, active);
    }

    /**
     * 检测异常并持久化到数据库
     */
    private void checkAndSaveAlerts(String vehicleId, double velocityGNSS, long timestamp) {
        boolean noDataAlert = isNoDataAlert(vehicleId);
        boolean lowSpeedAlert = isLowSpeedAlert(velocityGNSS);

        if (noDataAlert || lowSpeedAlert) {
            ActivityAlert alert = new ActivityAlert();
            alert.setVehicleId(vehicleId);
            alert.setNoDataAlert(noDataAlert);
            alert.setLowSpeedAlert(lowSpeedAlert);
            alert.setTimestamp(new Date(timestamp));
            alert.setAlertLevel(calculateAlertLevel(velocityGNSS));

            activityAlertMapper.insert(alert);
            logger.debug("保存告警: {}", alert);
        }
    }

    private boolean isNoDataAlert(String vehicleId) {
        Long lastUpdate = lastUpdateTime.get(vehicleId);
        boolean result = lastUpdate == null ||
                (System.currentTimeMillis() - lastUpdate) > NO_DATA_ALERT_THRESHOLD * 1000;
        if (result) {
            logger.debug("车辆 {} 触发无数据告警, 最后更新时间: {}", vehicleId,
                    lastUpdate != null ? new Date(lastUpdate) : "null");
        }
        return result;
    }

    private boolean isLowSpeedAlert(double velocityGNSS) {
        boolean result = velocityGNSS < LOW_SPEED_THRESHOLD;
        if (result) {
            logger.debug("触发低速告警, GNSS速度: {}, 阈值: {}",
                    velocityGNSS, LOW_SPEED_THRESHOLD);
        }
        return result;
    }

    private int calculateAlertLevel(double velocityGNSS) {
        return (velocityGNSS < 0.5) ? 2 : 1;
    }

    /**
     * 推送实时统计数据
     */
    private void pushStatistics() {
        long currentTime = System.currentTimeMillis();
        int[] counts = countActiveVehicles(currentTime);

        logger.info("推送统计数据 - 在线: {}, 活跃: {}", counts[0], counts[1]);

        String statsMessage = String.format(
                "{\"onlineCount\":%d,\"activeCount\":%d,\"timestamp\":\"%s\"}",
                counts[0], counts[1], new Date()
        );

        // 打印推送内容
        logger.debug("推送内容: {}", statsMessage);

        try {
            dataService.setPushContent("activity_alerts", statsMessage);
            logger.debug("推送成功");
        } catch (Exception e) {
            logger.error("推送统计数据失败: {}", e.getMessage(), e);
        }
    }

    private int[] countActiveVehicles(long currentTime) {
        int onlineCount = 0;
        int activeCount = 0;

        // 打印当前缓存状态
        logger.debug("当前缓存车辆数: {}", lastUpdateTime.size());

        // 先检查缓存是否为空
        if (lastUpdateTime.isEmpty()) {
            logger.warn("车辆状态缓存为空");
            return new int[]{0, 0};
        }

        // 放宽时间限制以便调试
        long timeWindow = CHECK_INTERVAL * 1000 * 5; // 扩大5倍检测时间窗口

        logger.debug("检测时间窗口: {}秒", timeWindow / 1000);

        for (Map.Entry<String, Long> entry : lastUpdateTime.entrySet()) {
            String vehicleId = entry.getKey();
            Long lastUpdate = entry.getValue();

            if (lastUpdate == null) {
                logger.debug("车辆 {} 的最后更新时间为null", vehicleId);
                continue;
            }

            long timeDiff = currentTime - lastUpdate;
            boolean inTimeWindow = timeDiff <= timeWindow;

            logger.debug("车辆 {} - 最后更新: {}, 当前时间: {}, 差值: {}秒, 在时间窗口内: {}",
                    vehicleId, new Date(lastUpdate), new Date(currentTime),
                    timeDiff / 1000, inTimeWindow);

            if (inTimeWindow) {
                onlineCount++;
                boolean active = isActive.getOrDefault(vehicleId, false);
                logger.debug("计数车辆: {} 在线, 活跃状态: {}", vehicleId, active);

                if (active) {
                    activeCount++;
                }
            }
        }

        logger.info("计数结果 - 在线: {}, 活跃: {}", onlineCount, activeCount);

        // 确保至少有一些数据返回（用于调试）
        if (onlineCount == 0 && !lastUpdateTime.isEmpty()) {
            logger.warn("没有在线车辆，但缓存不为空，强制返回一些数据进行调试");
            return new int[]{lastUpdateTime.size(), isActive.size()};
        }

        return new int[]{onlineCount, activeCount};
    }

    /**
     * 更新车辆在线时间
     */
    private void updateVehicleOnlineTime(String vehicleId) {
        long currentTime = CHECK_INTERVAL; // 每次更新增加检测周期的时间

        // 更新车辆在线时间
        long prevTime = vehicleOnlineTime.getOrDefault(vehicleId, 0L);
        long newTime = prevTime + currentTime;
        vehicleOnlineTime.put(vehicleId, newTime);

        logger.debug("更新车辆 {} 在线时间: {} -> {}", vehicleId, prevTime, newTime);

        // 模拟一些初始数据，确保有数据可以展示
        if (vehicleOnlineTime.size() < 10) {
            for (int i = 1; i <= 10; i++) {
                String vid = "vehicle" + i;
                if (!vehicleOnlineTime.containsKey(vid)) {
                    long time = (long)(Math.random() * 10000);
                    vehicleOnlineTime.put(vid, time);
                    logger.debug("添加模拟车辆 {} 在线时间: {}", vid, time);
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

        logger.debug("生成七天活跃度统计数据: {}", result);
        return result;
    }

    /**
     * 获取车辆在线时间排行
     */
    public List<Map<String, Object>> getVehicleOnlineTimeRanking(int limit) {
        List<Map<String, Object>> result = new ArrayList<>();

        // 如果没有实际数据，添加一些模拟数据
        if (vehicleOnlineTime.isEmpty()) {
            logger.info("车辆在线时间为空，添加模拟数据");
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

        logger.debug("生成车辆在线时间排行: {}", result);
        return result;
    }

    /**
     * 手动触发推送统计数据的方法，可用于调试
     */
    public void forcePushStatistics() {
        logger.info("手动触发推送统计数据");
        pushStatistics();
    }
}
