package org.swu.vehiclecloud.listener;

import cn.hutool.core.util.StrUtil;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;
import org.swu.vehiclecloud.entity.*;
import org.swu.vehiclecloud.event.MqttMessageEvent;
import org.swu.vehiclecloud.mapper.VehicleExpMapper;
import org.swu.vehiclecloud.service.DataService;

import java.io.IOException;
import java.sql.Timestamp;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.*;

/*
    此类的handleMqttMessage方法在接受到mqtt数据后将对数据进行解析，处理车辆异常状况
 */
@Component
public class ProcessExp {
    @Autowired
    private VehicleExpMapper vehicleExpMapper;

    @Autowired
    private DataService dataService;

    private static long previousTimestamp = 0;

    // 间隔执行监听方法
    private static int eventCount = 0;

    // 存储每个车辆的上一次数据，线程安全的Map
    private final ConcurrentMap<String, Map<String, Object>> vehicleDataCache = new ConcurrentHashMap<>();

    // 存储某个时间片异常车的数量，线程安全的Map
    private final ConcurrentMap<Long, Integer> numOfExpCar = new ConcurrentHashMap<>();

    // 线程池调度器
    private final ScheduledExecutorService scheduler = Executors.newSingleThreadScheduledExecutor();

    // 日志记录的类
    private static final Logger logger = LoggerFactory.getLogger(ProcessExp.class);

    // 处理json数据的类
    private static final ObjectMapper objectMapper = new ObjectMapper();

    @EventListener
    public void handleMqttMessage(MqttMessageEvent event) throws IOException, ParseException {
        eventCount++;

        // 每监听五条数据检测一次
        if (eventCount % 5 == 0) {
            try {
                // 初始化异常数量
                int numOfExp = 0;

                // 提取车辆数据
                JsonNode messageNode = objectMapper.readTree(event.getMessage());

                JsonNode bodyNode = messageNode.path("body");
                JsonNode positionNode = bodyNode.path("position");

                String vehicleId = bodyNode.path("vehicleId").asText();

                double steeringAngle = bodyNode.path("steeringAngle").asDouble();

                long timestampGNSS = bodyNode.path("timestampGNSS").asLong();
                long timestamp = messageNode.path("header").path("timestamp").asLong();

                double latitude = positionNode.path("latitude").asDouble();
                double longitude = positionNode.path("longitude").asDouble();

//                JsonNode headerNode = messageNode.get("header");
//                JsonNode bodyNode = messageNode.get("body");
//
//                long timestamp = headerNode.get("timestamp").asLong();
//
//                String vehicleId = bodyNode.get("vehicleId").asText();
//                double accelerationLon = bodyNode.get("accelerationLon").asDouble();
//                double accelerationLat = bodyNode.get("accelerationLat").asDouble();
//                double accelerationVer = bodyNode.get("accelerationVer").asDouble();
//
//                double velocityGNSS = bodyNode.get("velocityGNSS").asDouble();
//                double velocityCAN = bodyNode.get("velocityCAN").asDouble();
//
//                double engineSpeed = bodyNode.get("engineSpeed").asDouble();
//                double engineTorque = bodyNode.get("engineTorque").asDouble();
//
//                int brakeFlag = bodyNode.get("brakeFlag").asInt();
//                double brakePos = bodyNode.get("brakePos").asDouble();
//                double brakePressure = bodyNode.get("brakePressure").asDouble();
//
//                double steeringAngle = bodyNode.get("steeringAngle").asDouble();
//                double yawRate = bodyNode.get("yawRate").asDouble();
//
//                long timestampGNSS = bodyNode.get("timestampGNSS").asLong();
//                long timestamp3 = bodyNode.get("timestamp3").asLong();
//                long timestamp4 = bodyNode.get("timestamp4").asLong();
//
//                double longitude = bodyNode.get("longitude").asDouble();
//                double latitude = bodyNode.get("latitude").asDouble();

                // 将UTC时间戳转换为东八区(CST)时间戳
                Timestamp datestamp = UtcToCst(timestamp);

                if (numOfExpCar.isEmpty()) {
                    // 初始化当前时间片异常车辆数量为0
                    previousTimestamp = timestamp;
                    numOfExpCar.put(timestamp, 0);
                    // 启动一个任务，每10秒将这10秒内的异常车数量推送给前端
                    scheduler.scheduleAtFixedRate(this::pushNumOfExpData, 0, 10, TimeUnit.SECONDS);
                }

                // 将经纬度推给前端，不论是否异常
                Map<String, Object> pushLocationData = new HashMap<>();
                pushLocationData.put("vehicleId", vehicleId);
                pushLocationData.put("longitude", longitude);
                pushLocationData.put("latitude", latitude);
                dataService.setPushContent("1", objectMapper.writeValueAsString(pushLocationData));

                // 上一个时间片某辆车的数据
                Map<String, Object> previousVehicleData = vehicleDataCache.get(vehicleId);

                // 当前时间片某辆车的数据
                Map<String, Object> currentVehicleData = new HashMap<>();

                if (StrUtil.isEmptyIfStr(previousVehicleData)) {
                    // 如果是第一次接受该车辆的数据，则存储车辆数据到缓存当中, 用来判断转向异常和经纬度异常
                    currentVehicleData.put("timestamp", timestamp);
                    currentVehicleData.put("steeringAngle", steeringAngle);
//                    currentVehicleData.put("yawRate", yawRate);
                    currentVehicleData.put("longitude", longitude);
                    currentVehicleData.put("latitude", latitude);
                    vehicleDataCache.put(vehicleId, currentVehicleData);

                    // 启动一个任务，如果10秒之内没有收到该车辆的新数据，则从缓存中删除该车辆数据
                    scheduler.schedule(() -> {
                        Map<String, Object> cachedData = vehicleDataCache.get(vehicleId);
                        if (cachedData == null)
                            vehicleDataCache.remove(vehicleId);
                    }, 10, TimeUnit.SECONDS);
                } else {
                    if (Math.abs(timestamp - (Long) previousVehicleData.get("timestamp")) > Math.pow(10, 4)) {
                        // 经纬度异常检测
                        detectGeoLocationExp(vehicleId, longitude,
                                latitude, (double) previousVehicleData.get("longitude"),
                                (double) previousVehicleData.get("latitude"), datestamp,
                                numOfExp);
//                        // 检测横摆角速度与方向盘转角变化趋势是否匹配
//                        detectSwivelAngleExp(vehicleId, steeringAngle,
//                                yawRate, (double) previousVehicleData.get("steeringAngle"),
//                                (double) previousVehicleData.get("yawRate"), datestamp,
//                                numOfExp);
                        vehicleDataCache.replace(vehicleId, previousVehicleData, currentVehicleData);
                    }
                }

                // 加速度异常检测
//                detectAccelerationExp(vehicleId, accelerationLon, accelerationLat,
//                        accelerationVer, datestamp, numOfExp);

                // 速度异常检测
//                detectSpeedExp(vehicleId, velocityGNSS, velocityCAN, datestamp, numOfExp);

                // 发动机异常检测
//                detectEngineExp(vehicleId, engineSpeed, engineTorque, datestamp, numOfExp);

                // 制动异常检测
//                detectBrakeExp(vehicleId, brakeFlag, brakePos, brakePressure, datestamp, numOfExp);

                // 转向异常检测
                detectSteeringExp(vehicleId, steeringAngle, datestamp, numOfExp);

                // 时间戳异常检测
                detectTimestampExp(vehicleId, timestampGNSS, timestamp, datestamp, numOfExp);

                // 将对应时间片的异常车数量存入缓存
                // 使用 compute 来更新值
                numOfExpCar.compute(previousTimestamp, (key, currentValue) ->
                        (currentValue == null ? 0 : currentValue) + numOfExp
                );

            } catch (IOException e) {
                throw new IOException("Internal server error. Please try again later.");
            } catch (NullPointerException e) {
                throw new NullPointerException("Bad request. Missing required fields.");
            } catch (NumberFormatException e) {
                throw new NumberFormatException("Bad request. Invalid number format.");
            } catch (ParseException e) {
                throw new ParseException("Server error, timestamp parse failed", 1);
            }
        }
    }
//    private void detectAccelerationExp(String vehicleId, double accelerationLon,
//                                       double accelerationLat, double accelerationVer,
//                                       Timestamp timestamp, int numOfExp) throws JsonProcessingException {
//        // 判断加速度是否异常
//        if(isAccelerationExp(accelerationLon,accelerationLat,
//                accelerationVer)){
//            // 车辆有异常
//            numOfExp = 1;
//
//            // 创建加速度异常对象
//            AccelerationExp accelerationExp = new AccelerationExp(vehicleId, accelerationLon / 100,
//                    accelerationLat / 100, accelerationVer / 100,
//                    timestamp);
//
//            // 插入加速度异常对象
//            vehicleExpMapper.insertAccelerationExp(accelerationExp);
//
//            // 推送异常信息给前端
//            Map<String, Object> pushData = new HashMap<>();
//            pushData.put("vehicleId", vehicleId);
//            pushData.put("accelerationExp", true);
//            dataService.setPushContent("5", objectMapper.writeValueAsString(pushData));
//        }
//    }
//
//    private void detectSpeedExp(String vehicleId, double velocityGNSS,
//                                double velocityCAN, Timestamp timestamp,
//                                int numOfExp) throws JsonProcessingException {
//        // 判断速度是否异常
//        if(isSpeedExp(velocityGNSS, velocityCAN)){
//            // 车辆有异常
//            numOfExp = 1;
//
//            // 创建速度异常对象
//            SpeedExp speedExp = new SpeedExp(vehicleId, velocityGNSS / 100,
//                    velocityCAN / 100, timestamp);
//
//            // 插入速度异常对象
//            vehicleExpMapper.insertSpeedExp(speedExp);
//
//            // 推送异常信息给前端
//            Map<String, Object> pushData = new HashMap<>();
//            pushData.put("vehicleId", vehicleId);
//            pushData.put("speedExp", true);
//            dataService.setPushContent("6", objectMapper.writeValueAsString(pushData));
//        }
//    }
//
//    private void detectEngineExp(String vehicleId, double engineSpeed,
//                                 double engineTorque, Timestamp timestamp,
//                                 int numOfExp) throws JsonProcessingException {
//        // 判断发动机是否异常
//        if(isEngineExp(engineSpeed, engineTorque)){
//            // 车辆有异常
//            numOfExp = 1;
//
//            // 创建发动机异常对象
//            EngineExp engineExp = new EngineExp(vehicleId, engineSpeed,
//                    engineTorque / 100, timestamp);
//
//            // 插入发动机异常对象
//            vehicleExpMapper.insertEngineExp(engineExp);
//
//            // 推送异常信息给前端
//            Map<String, Object> pushData = new HashMap<>();
//            pushData.put("vehicleId", vehicleId);
//            pushData.put("engineExp", true);
//            dataService.setPushContent("7", objectMapper.writeValueAsString(pushData));
//        }
//    }
//
//    private void detectBrakeExp(String vehicleId, int brakeFlag,
//                                double brakePos, double brakePressure,
//                                Timestamp timestamp, int numOfExp) throws JsonProcessingException {
//        if(isBrakeExp(brakeFlag, brakePos, brakePressure)){
//            // 车辆有异常
//            numOfExp = 1;
//
//            // 将brakeFlag转换为boolean
//            boolean flag = brakeFlag != 0;
//
//            // 创建制动异常对象
//            BrakeExp brakeExp = new BrakeExp(vehicleId, flag,
//                    brakePos / 10, brakePressure / 100,
//                    timestamp);
//
//            // 插入制动异常对象
//            vehicleExpMapper.insertBrakeExp(brakeExp);
//
//            // 推送异常信息给前端
//            Map<String, Object> pushData = new HashMap<>();
//            pushData.put("vehicleId", vehicleId);
//            pushData.put("brakeExp", true);
//            dataService.setPushContent("7", objectMapper.writeValueAsString(pushData));
//        }
//    }

    private void detectSteeringExp(String vehicleId, double steeringAngle,
                                   Timestamp timestamp,
                                   int numOfExp) throws JsonProcessingException {
        if(isSteeringExp(steeringAngle)){
            // 车辆有异常
            numOfExp = 1;

            // 创建转向异常对象
            SteeringExp steeringExp = new SteeringExp(vehicleId, steeringAngle / 10000,
                    timestamp);

            // 插入转向异常对象
            vehicleExpMapper.insertSteeringExp(steeringExp);

            // 推送异常信息给前端
            Map<String, Object> pushData = new HashMap<>();
            pushData.put("vehicleId", vehicleId);
            pushData.put("steeringExp", true);
            dataService.setPushContent("3", objectMapper.writeValueAsString(pushData));
        }

    }

//    private void detectSwivelAngleExp(String vehicleId, double steeringAngle,
//                                      double yawRate, double previousSteeringAngle,
//                                      double previousYawRate, Timestamp timestamp,
//                                      int numOfExp) throws JsonProcessingException {
//        if(isSwivelAngleExp(steeringAngle, previousSteeringAngle, yawRate, previousYawRate)){
//            // 车辆有异常
//            numOfExp = 1;
//
//            // 创建转向异常对象
//            SteeringExp steeringExp = new SteeringExp(vehicleId, steeringAngle / 10000,
//                    yawRate / 100, timestamp);
//
//            // 插入转向异常对象
//            vehicleExpMapper.insertSteeringExp(steeringExp);
//
//            // 推送异常信息给前端
//            Map<String, Object> pushData = new HashMap<>();
//            pushData.put("vehicleId", vehicleId);
//            pushData.put("steeringExp", true);
//            dataService.setPushContent("8", objectMapper.writeValueAsString(pushData));
//        }
//    }

    private void detectTimestampExp(String vehicleId, long timestampGNSS,
                                    long timestamp, Timestamp datestamp,
                                    int numOfExp) throws JsonProcessingException, ParseException {
        if(isTimeStampExp(timestampGNSS, timestamp)){
            // 车辆有异常
            numOfExp = 1;

            // 将UTC时间戳转换为东八区(CST)时间戳
            Timestamp datestampGNSS = UtcToCst(timestampGNSS);

            // 创建时间戳异常对象
            TimestampExp timestampExp = new TimestampExp(vehicleId, datestampGNSS,
                    datestamp);

            // 插入时间戳异常对象
            vehicleExpMapper.insertTimestampExp(timestampExp);

            // 推送异常信息给前端
            Map<String, Object> pushData = new HashMap<>();
            pushData.put("vehicleId", vehicleId);
            pushData.put("timestampExp", true);
            dataService.setPushContent("4", objectMapper.writeValueAsString(pushData));
        }
    }

    private void detectGeoLocationExp(String vehicleId, double longitude,
                                      double latitude, double previousLongitude,
                                      double previousLatitude, Timestamp datestamp,
                                      int numOfExp) throws JsonProcessingException {
        if(isGeoLocationExp(longitude, latitude, previousLongitude, previousLatitude)){
            // 车辆有异常
            numOfExp = 1;

            // 创建地理位置异常对象
            GeoLocationExp geoLocationExp = new GeoLocationExp(vehicleId, longitude,
                    latitude, datestamp);

            // 插入地理位置异常对象
            vehicleExpMapper.insertGeoLocationExp(geoLocationExp);

            // 推送异常信息给前端
            Map<String, Object> pushData = new HashMap<>();
            pushData.put("vehicleId", vehicleId);
            pushData.put("geoLocationExp", true);
            dataService.setPushContent("5", objectMapper.writeValueAsString(pushData));
        }
    }

//    private boolean isAccelerationExp(double accelerationLon, double accelerationLat,
//                                      double accelerationVer) {
//        return accelerationLon > 500 || accelerationLon < -500
//                || accelerationLat > 500 || accelerationLat < -500
//                || accelerationVer > 500 || accelerationVer < -500;
//    }
//
//    private boolean isSpeedExp(double velocityGNSS, double velocityCAN) {
//        return Math.abs(velocityGNSS - velocityCAN) >= 5;
//    }
//
//    private boolean isEngineExp(double engineSpeed, double engineTorque) {
//        return engineSpeed < 50 && engineTorque >= 50000;
//    }
//
//    private boolean isBrakeExp(int brakeFlag, double brakePos, double brakePressure) {
//        return (brakeFlag == 1 && brakePos < 50 && brakePressure < 5000) ||
//                (brakeFlag == 0 && brakePos != 0 && brakePressure != 0);
//    }

    private boolean isSteeringExp(double steeringAngle) {
        return Math.abs(steeringAngle) > Math.pow(10, 7);

    }

//    private boolean isSwivelAngleExp(double steeringAngle, double previousSteeringAngle,
//                                     double yawRate, double previousYawRate) {
//        return (Math.abs(steeringAngle - previousSteeringAngle) <= 5 * Math.pow(10, 4)
//                && Math.abs(yawRate - previousYawRate) >= 15 * Math.pow(10, 2)) ||
//                (Math.abs(steeringAngle - previousSteeringAngle) >= 30 * Math.pow(10, 4)
//                        && Math.abs(yawRate - previousYawRate) <= 5 * Math.pow(10, 2));
//    }

    private boolean isTimeStampExp(long timestampGNSS, long timestamp) {
        return Math.abs(timestampGNSS - timestamp) > 100;
    }

    private boolean isGeoLocationExp(double longitude, double latitude,
                                     double previousLongitude, double previousLatitude) {
        // 计算经度差值
        double longitudeDiff = Math.abs(longitude - previousLongitude);

        // 如果经度差值大于180，取180到360之间的最小差值
        if (longitudeDiff > 180) {
            longitudeDiff = 360 - longitudeDiff;
        }

        // 计算纬度差值
        double latitudeDiff = Math.abs(latitude - previousLatitude);

        // 判断是否超过阈值
        if (longitudeDiff > 0.005 || latitudeDiff > 0.004) {
            return true;
        }

        return false;
    }

    /**
     * 将 UTC 时间戳转换为东八区 Date 对象
     * @param timestamp UTC 时间戳（单位：秒）
     * @return Date 对象（东八区时间）
     */
    private Timestamp UtcToCst(long timestamp) throws ParseException {
        // 创建内部变量，避免修改参数的值
        long datestamp = timestamp / 1000;

        // 将毫秒转换为秒
        datestamp %= 1000;

        // 将 UTC 时间戳转换为 Instant 对象
        Instant instant = Instant.ofEpochSecond(datestamp);

        // 将 UTC 时间戳转换为东八区（Asia/Shanghai）的 ZonedDateTime 对象
        ZonedDateTime beijingTime = instant.atZone(ZoneId.of("Asia/Shanghai"));

        // 将 ZonedDateTime 转换为 java.util.Date 并返回
        Date date = Date.from(beijingTime.toInstant());

        // 格式化 Date 对象为 "yyyy-MM-dd HH:mm:ss" 格式
        SimpleDateFormat outputFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");

        Date parsedDate = outputFormat.parse(outputFormat.format(date));

        // 将 Date 对象转换为 Timestamp并返回
        return new Timestamp(date.getTime());
    }

    private void pushNumOfExpData() {
        try {
            int numOfExp = numOfExpCar.get(previousTimestamp);
            Map<String, Object> pushData = new HashMap<>();
            pushData.put("numOfExp", numOfExp);
            // Convert the map to JSON and push the content to the frontend
            dataService.setPushContent("2", objectMapper.writeValueAsString(pushData));

            // Clear the map after sending the data
            numOfExpCar.clear();
        } catch (JsonProcessingException e) {
            // Log the exception or handle it in another way
            logger.error("Error while processing JSON for push data: {}", e.getMessage());
        }
    }
}
