package org.swu.vehiclecloud.listener;

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
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.Date;

@Component
public class MqttMessageListener {
    @Autowired
    private VehicleExpMapper vehicleExpMapper;

    @Autowired
    private DataService dataService;

    private static final Logger logger = LoggerFactory.getLogger(MqttMessageListener.class);

    private static final ObjectMapper objectMapper = new ObjectMapper();

    @EventListener
    public void handleMqttMessage(MqttMessageEvent event) throws IOException {
        try{
            logger.info("Event received - Topic: {}, Message: {}",
                    event.getTopic(), event.getMessage());

            // 根据不同的topic进行不同的处理
            if (event.getTopic().contains("temperature")) {
                handleTemperatureMessage(event);
            } else if (event.getTopic().contains("alert")) {
                handleAlertMessage(event);
            }

            // 提取车辆数据
            JsonNode messageNode = objectMapper.readTree(event.getMessage());

            JsonNode headerNode = messageNode.get("header");
            JsonNode bodyNode = messageNode.get("body");

            long timestamp = headerNode.get("timestamp").asLong();

            String vehicleId = bodyNode.get("vehicleId").asText();
            double accelerationLon = bodyNode.get("accelerationLon").asDouble();
            double accelerationLat = bodyNode.get("accelerationLat").asDouble();
            double accelerationVer = bodyNode.get("accelerationVer").asDouble();

            double velocityGNSS = bodyNode.get("velocityGNSS").asDouble();
            double velocityCAN = bodyNode.get("velocityCAN").asDouble();

            int engineSpeed = bodyNode.get("engineSpeed").asInt();
            int engineTorque = bodyNode.get("engineTorque").asInt();

            int brakeFlag = bodyNode.get("brakeFlag").asInt();
            int brakePos = bodyNode.get("brakePos").asInt();
            int brakePressure = bodyNode.get("brakePressure").asInt();

            int steeringAngle = bodyNode.get("steeringAngle").asInt();
            int yawRate = bodyNode.get("yawRate").asInt();

            long timestampGNSS = bodyNode.get("timestampGNSS").asLong();
            long timestamp3 = bodyNode.get("timestamp3").asLong();
            long timestamp4 = bodyNode.get("timestamp4").asLong();

            double longitude = bodyNode.get("longitude").asDouble();
            double latitude = bodyNode.get("latitude").asDouble();

            // 将UTC时间戳转换为东八区(CST)时间戳
            Date datestamp = UtcToCst(timestamp);

            // 将经纬度推给前端，不论是否异常
            dataService.setPushContent("1", "编号为" + vehicleId
                                        + "的车辆的经纬度为" + "(" + longitude + "," + latitude + ")");

            // 加速度异常检测
            detectAccelerationExp(vehicleId, accelerationLon,accelerationLat,
                                    accelerationVer, datestamp);

            // 速度异常检测
            detectSpeedExp(vehicleId, velocityGNSS, velocityCAN, datestamp);

            // 发动机异常检测
            detectEngineExp(vehicleId, engineSpeed, engineTorque, datestamp);

            // 制动异常检测
            detectBrakeExp(vehicleId, brakeFlag, brakePos, brakePressure, datestamp);

            // 转向异常检测
            detectSteeringExp(vehicleId, steeringAngle, yawRate, datestamp);

            // 时间戳异常检测
            detectTimestampExp(vehicleId, timestampGNSS, timestamp3, timestamp4, datestamp);

        }catch(IOException e){
            throw new IOException("Internal server error. Please try again later.");
        }catch(NullPointerException e){
            throw new NullPointerException("Bad request. Missing required fields.");
        }catch(NumberFormatException e){
            throw new NumberFormatException("Bad request. Invalid number format.");
        }
    }

    private void detectAccelerationExp(String vehicleId, double accelerationLon,
                                       double accelerationLat, double accelerationVer,
                                       Date timestamp) {
        // 判断加速度是否异常
        if(isAccelerationExp(accelerationLon,accelerationLat,
                accelerationVer)){
            // 创建加速度异常对象
            AccelerationExp vehicleExp = new AccelerationExp(vehicleId, accelerationLon,
                                                            accelerationLat, accelerationVer,
                                                            timestamp);

            // 插入加速度异常对象
            vehicleExpMapper.insert(vehicleExp);

            // 推送异常信息给前端
            dataService.setPushContent("1","编号为" + vehicleId + "的车辆加速度异常");
        }
    }

    private void detectSpeedExp(String vehicleId, double velocityGNSS,
                                double velocityCAN, Date timestamp) {
        // 判断速度是否异常
        if(isSpeedExp(velocityGNSS, velocityCAN)){
            // 创建速度异常对象
            SpeedExp speedExp = new SpeedExp(vehicleId, velocityGNSS,
                                            velocityCAN, timestamp);

            // 插入速度异常对象
            vehicleExpMapper.insert(speedExp);

            // 推送异常信息给前端
            dataService.setPushContent("1","编号为" + vehicleId + "的车辆速度异常");
        }
    }

    private void detectEngineExp(String vehicleId, int engineSpeed,
                                 int engineTorque, Date timestamp) {
        // 判断发动机是否异常
        if(isEngineExp(engineSpeed, engineTorque)){
            // 创建发动机异常对象
            EngineExp engineExp = new EngineExp(vehicleId, engineSpeed,
                                                engineTorque, timestamp);

            // 插入发动机异常对象
            vehicleExpMapper.insert(engineExp);

            // 推送异常信息给前端
            dataService.setPushContent("1","编号为" + vehicleId + "的车辆发动机异常");
        }
    }

    private void detectBrakeExp(String vehicleId, int brakeFlag,
                                int brakePos, int brakePressure,
                                Date timestamp) {
        if(isBrakeExp(brakeFlag, brakePos, brakePressure)){
            // 将brakeFlag转换为boolean
            boolean flag = brakeFlag != 0;

            // 创建制动异常对象
            BrakeExp brakeExp = new BrakeExp(vehicleId, flag,
                                            brakePos, brakePressure,
                                            timestamp);

            // 插入制动异常对象
            vehicleExpMapper.insert(brakeExp);

            // 推送异常信息给前端
            dataService.setPushContent("1","编号为" + vehicleId + "的车辆制动异常");
        }
    }

    private void detectSteeringExp(String vehicleId, int steeringAngle,
                                   int yawRate, Date timestamp) {
        if(isSteeringExp(steeringAngle, yawRate)){
            // 创建转向异常对象
            SteeringExp steeringExp = new SteeringExp(vehicleId, steeringAngle,
                                                        yawRate, timestamp);

            // 插入转向异常对象
            vehicleExpMapper.insert(steeringExp);

            // 推送异常信息给前端
            dataService.setPushContent("1","编号为" + vehicleId + "的车辆转向异常");
        }

    }

    private void detectTimestampExp(String vehicleId, long timestampGNSS,
                                    long timestamp3, long timestamp4, Date timestamp) {
        if(isTimeStampExp(timestampGNSS, timestamp3, timestamp4)){
            // 将UTC时间戳转换为东八区(CST)时间戳
            Date datestampGNSS = UtcToCst(timestampGNSS);

            // 将UTC时间戳转换为东八区(CST)时间戳
            Date datestamp3 = UtcToCst(timestamp3);

            // 将UTC时间戳转换为东八区(CST)时间戳
            Date datestamp4 = UtcToCst(timestamp4);

            // 创建时间戳异常对象
            TimestampExp timestampExp = new TimestampExp(vehicleId, datestampGNSS,
                                                        datestamp3, datestamp4,
                                                        timestamp);

            // 插入时间戳异常对象
            vehicleExpMapper.insert(timestampExp);

            // 推送异常信息给前端
            dataService.setPushContent("1","编号为" + vehicleId + "的车辆时间戳异常");
        }
    }

    private boolean isAccelerationExp(double accelerationLon, double accelerationLat,
                                            double accelerationVer) {
        return accelerationLon > 500 || accelerationLon < -500
               || accelerationLat > 500 || accelerationLat < -500
               || accelerationVer > 500 || accelerationVer < -500;
    }

    private boolean isSpeedExp(double velocityGNSS, double velocityCAN) {
        return Math.abs(velocityGNSS - velocityCAN) >= 5;
    }

    private boolean isEngineExp(int engineSpeed, int engineTorque) {
        return engineSpeed < 50 && engineTorque >= 50000;
    }

    private boolean isBrakeExp(int brakeFlag, int brakePos, int brakePressure) {
        return (brakeFlag == 1 && brakePos < 50 && brakePressure < 5000) ||
               (brakeFlag == 0 && brakePos != 0 && brakePressure != 0);
    }

    private boolean isSteeringExp(int steeringAngle, int yawRate) {
        return steeringAngle > Math.pow(10, 7) || steeringAngle <  -(Math.pow(10, 7))
               || yawRate > Math.pow(10, 4) || yawRate <  -(Math.pow(10, 4));
    }

    private boolean isTimeStampExp(long timestampGNSS, long timestamp3, long timestamp4) {
        return Math.abs(timestampGNSS - timestamp3) > 100 ||
               Math.abs(timestampGNSS - timestamp4) > 100 ||
               Math.abs(timestamp4 - timestampGNSS) > 100;
    }

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

    private void handleTemperatureMessage(MqttMessageEvent event) {
        // 处理温度数据
        logger.info("Processing temperature data: {}", event.getMessage());
    }

    private void handleAlertMessage(MqttMessageEvent event) {
        // 处理警报数据
        logger.warn("Processing alert: {}", event.getMessage());
    }
}