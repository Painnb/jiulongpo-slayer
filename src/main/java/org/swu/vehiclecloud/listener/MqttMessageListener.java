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
     * 系统启动时初始化一些测试数据
     * 只初始化与数据库匹配的5辆车
     */
    @PostConstruct
    public void init() {
        logger.info("初始化MqttMessageListener，添加测试车辆数据（限制为5辆）");
        // 添加初始模拟数据，确保系统启动时有数据，但只添加5辆车
        for (int i = 1; i <= 5; i++) {
            String vehicleId = "vehicle" + i;
            long timestamp = System.currentTimeMillis();
            double velocity = 5.0 + Math.random() * 10.0;  // 随机速度，确保大于低速阈值

            // 更新车辆状态缓存
            lastUpdateTime.put(vehicleId, timestamp);
            lastVelocityGNSS.put(vehicleId, velocity);
            lastVelocityCAN.put(vehicleId, velocity);
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

            // 解析嵌套的数据结构
            try {
                // 获取dataContent子对象
                Map<String, Object> dataContent = (Map<String, Object>) payload.get("dataContent");
                if (dataContent == null) {
                    logger.warn("消息中没有dataContent字段");
                    return;
                }

                // 获取position子对象
                Map<String, Object> position = (Map<String, Object>) dataContent.get("position");
                if (position == null) {
                    logger.debug("消息中没有position字段");
                    // 继续处理，因为position可能是可选的
                }

                // 从dataContent中提取必要字段
                String vehicleId = (String) dataContent.get("vehicleId");
                if (vehicleId == null || vehicleId.trim().isEmpty()) {
                    logger.warn("消息中vehicleId为空");
                    return;
                }

                // 从各种可能的位置提取速度信息
                double velocityGNSS = parseDouble(dataContent.get("velocityGNSS"));
                double velocityCAN = velocityGNSS; // 默认设置为相同值，除非有特定CAN速度
                
                // 如果有特定的CAN速度，则使用它
                if (dataContent.containsKey("velocityCAN")) {
                    velocityCAN = parseDouble(dataContent.get("velocityCAN"));
                }

                // 从payload和dataContent都尝试获取时间戳
                long timestamp = parseTimestamp(payload.get("timestamp"));
                if (timestamp == 0) {
                    timestamp = parseTimestamp(dataContent.get("timestampGNSS"));
                    if (timestamp == 0) {
                        timestamp = System.currentTimeMillis();
                    }
                }

                logger.debug("解析消息: 车辆ID={}, GNSS速度={}, CAN速度={}, 时间戳={}",
                        vehicleId, velocityGNSS, velocityCAN, new Date(timestamp));

                // 更新车辆状态缓存
                updateVehicleStatus(vehicleId, velocityGNSS, velocityCAN, timestamp);

                // 检测并保存异常数据
                checkAndSaveAlerts(vehicleId, velocityGNSS, velocityCAN, timestamp);

                // 更新车辆在线时间
                updateVehicleOnlineTime(vehicleId);

                // 推送实时统计信息
                pushStatistics();

            } catch (ClassCastException e) {
                logger.error("消息格式错误，无法解析嵌套对象: {}", e.getMessage());
                logger.debug("收到的消息内容: {}", payload);
            }

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
        
        try {
            if (obj instanceof Number) {
                return ((Number) obj).doubleValue();
            } else if (obj instanceof String) {
                return Double.parseDouble((String) obj);
            } else {
                logger.warn("无法将类型 {} 解析为double: {}", obj.getClass(), obj);
                return 0.0;
            }
        } catch (Exception e) {
            logger.warn("解析为double时出错: {} - {}", obj, e.getMessage());
            return 0.0;
        }
    }

    private long parseTimestamp(Object obj) {
        if (obj == null) {
            logger.warn("解析空对象为时间戳");
            return 0; // 返回0，让调用方可以检查并使用替代时间戳
        }
        
        try {
            if (obj instanceof Number) {
                return ((Number) obj).longValue();
            } else if (obj instanceof String) {
                return Long.parseLong((String) obj);
            } else if (obj instanceof Date) {
                return ((Date) obj).getTime();
            } else {
                logger.warn("无法将类型 {} 解析为时间戳: {}", obj.getClass(), obj);
                return 0;
            }
        } catch (Exception e) {
            logger.warn("解析为时间戳时出错: {} - {}", obj, e.getMessage());
            return 0;
        }
    }

    private void updateVehicleStatus(String vehicleId, double velocityGNSS, double velocityCAN, long timestamp) {
        logger.debug("更新车辆状态: ID={}, GNSS速度={}, CAN速度={}, 时间戳={}",
                vehicleId, velocityGNSS, velocityCAN, new Date(timestamp));

        lastUpdateTime.put(vehicleId, timestamp);
        lastVelocityGNSS.put(vehicleId, velocityGNSS);
        lastVelocityCAN.put(vehicleId, velocityCAN);
        boolean active = velocityGNSS >= LOW_SPEED_THRESHOLD && velocityCAN >= LOW_SPEED_THRESHOLD;
        isActive.put(vehicleId, active);

        logger.debug("车辆 {} 活跃状态: {}", vehicleId, active);
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

    private boolean isLowSpeedAlert(double velocityGNSS, double velocityCAN) {
        boolean result = velocityGNSS < LOW_SPEED_THRESHOLD || velocityCAN < LOW_SPEED_THRESHOLD;
        if (result) {
            logger.debug("触发低速告警, GNSS速度: {}, CAN速度: {}, 阈值: {}",
                    velocityGNSS, velocityCAN, LOW_SPEED_THRESHOLD);
        }
        return result;
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

        // 放宽时间限制以便调试，但不要太宽，使得统计更准确
        long timeWindow = CHECK_INTERVAL * 1000 * 2; // 扩大2倍检测时间窗口

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

        // 返回真实的统计值
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

        // 车辆在线时间统计不需要特别添加模拟数据，因为实际处理消息时会更新
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
            for (int i = 1; i <= 5; i++) { // 添加5辆车
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
    
    /**
     * 清除过期车辆，只保留最近活跃的车辆
     * 可以定期调用此方法，确保统计显示当前真实活跃的车辆
     */
    public void cleanupExpiredVehicles() {
        logger.info("清理过期车辆数据");
        long currentTime = System.currentTimeMillis();
        long expireTime = 60 * 1000; // 60秒不活跃则清除
        
        // 收集需要删除的车辆ID
        List<String> expiredVehicles = new ArrayList<>();
        
        for (Map.Entry<String, Long> entry : lastUpdateTime.entrySet()) {
            String vehicleId = entry.getKey();
            Long lastUpdate = entry.getValue();
            
            if (lastUpdate == null || (currentTime - lastUpdate) > expireTime) {
                expiredVehicles.add(vehicleId);
            }
        }
        
        // 从各个缓存中删除过期车辆
        for (String vehicleId : expiredVehicles) {
            lastUpdateTime.remove(vehicleId);
            lastVelocityGNSS.remove(vehicleId);
            lastVelocityCAN.remove(vehicleId);
            isActive.remove(vehicleId);
            
            // 车辆在线时间统计不删除，因为这是累计统计
        }
        
        logger.info("清理完成，删除 {} 辆过期车辆，当前缓存车辆数: {}", 
                    expiredVehicles.size(), lastUpdateTime.size());
    }
    
    /**
     * 打印当前的所有缓存状态，用于调试
     */
    public void printCacheStatus() {
        logger.info("============= 缓存状态 =============");
        logger.info("车辆总数: {}", lastUpdateTime.size());
        
        for (String vehicleId : lastUpdateTime.keySet()) {
            logger.info("车辆 ID: {}", vehicleId);
            logger.info("  最后更新时间: {}", 
                       lastUpdateTime.get(vehicleId) != null ? 
                       new Date(lastUpdateTime.get(vehicleId)) : "null");
            logger.info("  GNSS速度: {}", lastVelocityGNSS.get(vehicleId));
            logger.info("  CAN速度: {}", lastVelocityCAN.get(vehicleId));
            logger.info("  活跃状态: {}", isActive.get(vehicleId));
            logger.info("  累计在线时间: {}秒", vehicleOnlineTime.getOrDefault(vehicleId, 0L) / 1000);
            logger.info("-----------------------------------");
        }
        logger.info("====================================");
    }
}
