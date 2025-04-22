package org.swu.vehiclecloud.listener;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.swu.vehiclecloud.entity.ActivityAlert;
import org.swu.vehiclecloud.event.MqttMessageEvent;
import org.swu.vehiclecloud.mapper.ActivityAlertMapper;
import org.swu.vehiclecloud.service.DataService;

import java.sql.Timestamp;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentSkipListSet;

@Component
public class MqttMessageListener {
    @Autowired
    private ActivityAlertMapper activityAlertMapper;

    @Autowired
    private DataService dataService;

    // 处理json数据的类
    private static final ObjectMapper objectMapper = new ObjectMapper();

    // 记录在线车辆的线程安全的Set
    private final ConcurrentSkipListSet<String> vehicleOnlineSet = new ConcurrentSkipListSet<>();

    // 记录活跃车辆的线程安全的Set
    private final ConcurrentSkipListSet<String> vehicleActivitySet = new ConcurrentSkipListSet<>();

    /**
     * 处理MQTT消息事件
     */
    @EventListener
    public void handleMqttMessage(MqttMessageEvent event) throws ParseException {
        try {
            // 提取车辆数据
            Map<String, Object> payload = event.getMessage();

            // 获取 header 部分
            Map<String, Object> header = (Map<String, Object>) payload.get("header");

            // 获取 body 部分
            Map<String, Object> body = (Map<String, Object>) payload.get("body");

            // 获取 vehicleId
            String vehicleId = (String) body.get("vehicleId");

            // 获取 velocityGNSS
            double velocityGNSS = (double) body.get("velocityGNSS");

            // 获取 timestamp (来自header)
            long timestamp = (long) header.get("timestamp");

            vehicleOnlineSet.add(vehicleId);

            if(velocityGNSS <= 1.0){
                // 车辆不活跃
                activityAlertMapper.insertActivityAlert(new ActivityAlert(vehicleId, false, true, UtcToCst(timestamp)));
            }else{
                // 车辆活跃
                activityAlertMapper.insertActivityAlert(new ActivityAlert(vehicleId, false, false, UtcToCst(timestamp)));
                vehicleActivitySet.add(vehicleId);
            }
        } catch (NullPointerException e) {
            throw new NullPointerException("Bad request. Missing required fields.");
        } catch (NumberFormatException e) {
            throw new NumberFormatException("Bad request. Invalid number format.");
        } catch (ParseException e) {
            throw new ParseException("Server error, timestamp parse failed", 1);
        }
    }

    // 每 10 秒推送一次
    @Scheduled(fixedRate = 10000)
    public void pushActivityData() throws JsonProcessingException {
        Map<String, Object> pushOnlineData = new HashMap<>();
        pushOnlineData.put("numOfOnline", vehicleOnlineSet.size());
        dataService.setPushContent("10", objectMapper.writeValueAsString(pushOnlineData));

        Map<String, Object> pushActivityData = new HashMap<>();
        pushActivityData.put("numOfActivity", vehicleActivitySet.size());
        dataService.setPushContent("11", objectMapper.writeValueAsString(pushActivityData));

        vehicleOnlineSet.clear();
        vehicleActivitySet.clear();
    }
    /**
     * 将 UTC 时间戳转换为东八区 Date 对象
     * @param timestamp UTC 时间戳（单位：秒）
     * @return Date 对象（东八区时间）
     */
    private Timestamp UtcToCst(long timestamp) throws ParseException {
        // 将时间戳转换为Date对象
        Date date = new Date(timestamp);

        // 创建SimpleDateFormat对象，定义格式为DATETIME格式
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");

        // 将Date对象格式化为字符串
        String formattedDate = sdf.format(date);

        return Timestamp.valueOf(formattedDate);
    }
}