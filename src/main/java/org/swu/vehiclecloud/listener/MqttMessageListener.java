package org.swu.vehiclecloud.listener;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;
import org.swu.vehiclecloud.entity.AccelerationExp;
import org.swu.vehiclecloud.entity.EngineExp;
import org.swu.vehiclecloud.entity.SpeedExp;
import org.swu.vehiclecloud.event.MqttMessageEvent;
import org.swu.vehiclecloud.mapper.VehicleExpMapper;
import org.swu.vehiclecloud.service.DataService;

import java.io.IOException;
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

            String vehicleId = bodyNode.get("vehicleId").asText();
            double accelerationLon = bodyNode.get("accelerationLon").asDouble();
            double accelerationLat = bodyNode.get("accelerationLat").asDouble();
            double accelerationVer = bodyNode.get("accelerationVer").asDouble();
            double velocityGNSS = bodyNode.get("velocityGNSS").asDouble();
            double velocityCAN = bodyNode.get("velocityCAN").asDouble();
            int engineSpeed = bodyNode.get("engineSpeed").asInt();
            int engineTorque = bodyNode.get("engineTorque").asInt();
            long timestamp = headerNode.get("timestamp").asLong();

            // 加速度异常检测
            detectAccelerationExp(vehicleId, accelerationLon,accelerationLat,
                                    accelerationVer, timestamp);

            // 速度异常检测
            detectSpeedExp(vehicleId, velocityGNSS, velocityCAN, timestamp);

            // 发动机异常检测
            detectEngineExp(vehicleId, engineSpeed, engineTorque, timestamp);
        }catch(IOException e){
            throw new IOException("Internal server error. Please try again later.");
        }catch(NullPointerException e){
            throw new NullPointerException("Bad request. Missing required fields.");
        }catch(NumberFormatException e){
            throw new NumberFormatException("Bad request. Invalid number format.");
        }
    }

    private void detectAccelerationExp(String vehicleId, double accelerationLon, double accelerationLat, double accelerationVer, long timestamp) {
        // 判断加速度是否异常
        if(isAccelerationExp(accelerationLon,accelerationLat,
                accelerationVer)){
            // 类型转换，输出格式可能有bug
            Date date = new Date(timestamp);

            // 创建加速度异常对象
            AccelerationExp vehicleExp = new AccelerationExp(vehicleId, accelerationLon,
                    accelerationLat, accelerationVer, date);

            // 插入加速度异常对象
            vehicleExpMapper.insert(vehicleExp);

            // 推送异常信息给前端
            dataService.setPushContent("1",vehicleId + "号车辆加速度异常");
        }
    }

    private void detectSpeedExp(String vehicleId, double velocityGNSS, double velocityCAN, long timestamp) {
        // 判断速度是否异常
        if(isSpeedExp(velocityGNSS, velocityCAN)){
            // 类型转换，输出格式可能有bug
            Date date = new Date(timestamp);

            // 创建速度异常对象
            SpeedExp speedExp = new SpeedExp(vehicleId, velocityGNSS,velocityCAN, date);

            // 插入速度异常对象
            vehicleExpMapper.insert(speedExp);

            // 推送异常信息给前端
            dataService.setPushContent("1",vehicleId + "号车辆加速度异常");
        }
    }

    private void detectEngineExp(String vehicleId, int engineSpeed, int engineTorque, long timestamp) {
        // 判断发动机是否异常
        if(isEngineExp(engineSpeed, engineTorque)){
            // 类型转换，输出格式可能有bug
            Date date = new Date(timestamp);

            // 创建发动机异常对象
            EngineExp engineExp = new EngineExp(vehicleId, engineSpeed, engineTorque, date);

            // 插入发动机异常对象
            vehicleExpMapper.insert(engineExp);

            // 推送异常信息给前端
            dataService.setPushContent("1",vehicleId + "号车辆加速度异常");
        }
    }

    private boolean isAccelerationExp(double accelerationLon, double accelerationLat,
                                            double accelerationVer) {
        return accelerationLon > 500 || accelerationLon < -500 || accelerationLat > 500 || accelerationLat < -500
                || accelerationVer > 500 || accelerationVer < -500;
    }

    private boolean isSpeedExp(double velocityGNSS, double velocityCAN) {
        return Math.abs(velocityGNSS - velocityCAN) >= 5;
    }

    private boolean isEngineExp(int engineSpeed, int engineTorque) {
        return engineSpeed < 50 && engineTorque >= 50000;
        //sse
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