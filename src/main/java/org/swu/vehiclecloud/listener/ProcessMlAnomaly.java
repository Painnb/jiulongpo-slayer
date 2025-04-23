package org.swu.vehiclecloud.listener;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;
import org.swu.vehiclecloud.entity.MlExpcetion;
import org.swu.vehiclecloud.event.MqttMessageEvent;
import org.swu.vehiclecloud.mapper.VehicleExpMapper;

import java.io.IOException;
import java.sql.Timestamp;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import org.springframework.http.*;
import org.swu.vehiclecloud.service.DataService;

@Component
public class ProcessMlAnomaly {
    @Autowired
    private VehicleExpMapper vehicleExpMapper;

    // 处理json数据的类
    private static final ObjectMapper objectMapper = new ObjectMapper();

    @Autowired
    private DataService dataService;

    // 每5次执行一次
    private static int eventCount = 0;

    // Create a RestTemplate instance
    private final RestTemplate restTemplate = new RestTemplate();

    @EventListener
    public void handleMqttMessage(MqttMessageEvent event) throws IOException, ParseException {
        eventCount++;
        if(eventCount % 5 == 0) {
            try {
                // 提取车辆数据
                Map<String, Object> payload = event.getMessage();

                // 获取 header 部分
                Map<String, Object> header = (Map<String, Object>) payload.get("header");

                // 获取 timestamp (来自header)
                long timestamp = (long) header.get("timestamp");

                // 调用机器学习检测模块
                String apiUrl = "http://127.0.0.1:8081/detect-anomaly/";
                HttpHeaders headers = new HttpHeaders();
                headers.setContentType(MediaType.APPLICATION_JSON);
                HttpEntity<Map<String, Object>> request = new HttpEntity<>(payload, headers);

                // 接受返回值
                ResponseEntity<Map> responseEntity = restTemplate.exchange(apiUrl, HttpMethod.POST, request, Map.class);

                // 解析返回值
                Map<String, Object> responseBody = responseEntity.getBody();
                String vehicleId = (String) responseBody.get("vehicle_id");
                double mse = (double) responseBody.get("mse");

                if(mse > 0.1){
                    // 车辆异常
                    vehicleExpMapper.insertMlExp(new MlExpcetion(vehicleId, UtcToCst(timestamp), mse));
                    Map<String, Object> MlData = new HashMap<>();
                    MlData.put("vehicleId", vehicleId);
                    MlData.put("mlExp", true);
                    dataService.setPushContent("7", objectMapper.writeValueAsString(MlData));
                }else{
                    // 车辆正常
                    Map<String, Object> MlData = new HashMap<>();
                    MlData.put("vehicleId", vehicleId);
                    MlData.put("mlExp", false);
                    dataService.setPushContent("7", objectMapper.writeValueAsString(MlData));
                }
            } catch (NullPointerException e) {
                throw new NullPointerException("Bad request. Missing required fields.");
            } catch (NumberFormatException e) {
                throw new NumberFormatException("Bad request. Invalid number format.");
            } catch (ParseException e) {
                throw new ParseException("Server error, timestamp parse failed", 1);
            }
        }
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
