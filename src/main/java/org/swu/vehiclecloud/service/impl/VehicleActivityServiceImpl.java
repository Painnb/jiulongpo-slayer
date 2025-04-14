package org.swu.vehiclecloud.service.impl;

import org.springframework.stereotype.Service;
import org.swu.vehiclecloud.mapper.VehicleActivityMapper;
import org.swu.vehiclecloud.service.VehicleActivityService;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class VehicleActivityServiceImpl implements VehicleActivityService {

    private final VehicleActivityMapper vehicleActivityMapper;

    private static final List<String> ACTIVITY_TABLES = Arrays.asList(
            "acceleration_exp",
            "brake_exp",
            "engine_exp",
            "geo_location_exp",
            "speed_exp",
            "steering_exp",
            "timestamp_exp"
    );

    public VehicleActivityServiceImpl(VehicleActivityMapper vehicleActivityMapper) {
        this.vehicleActivityMapper = vehicleActivityMapper;
    }

    @Override
    public Map<String, Object> getVehicleActivityData() {
        // 1. 获取原始数据
        Object[][] rawData = this.queryRawActivityData();

        // 2. 转换为对象格式
        return this.convertToResponseFormat(rawData);
    }

    private Object[][] queryRawActivityData() {
        List<Map<String, Object>> activityList =
                vehicleActivityMapper.selectAllVehicleActivities(ACTIVITY_TABLES);

        Object[][] result = new Object[activityList.size() + 1][2];
        result[0] = new Object[]{"amount", "product"};

        for (int i = 0; i < activityList.size(); i++) {
            Map<String, Object> record = activityList.get(i);
            result[i + 1] = new Object[]{
                    record.get("activity_count"),
                    "Vehicle " + record.get("vehicleId")
            };
        }

        return result;
    }

    private Map<String, Object> convertToResponseFormat(Object[][] rawData) {
        Map<String, Object> response = new HashMap<>();
        response.put("headers", rawData[0]);

        Object[] data = new Object[rawData.length - 1];
        for (int i = 1; i < rawData.length; i++) {
            Map<String, Object> item = new HashMap<>();
            item.put("amount", rawData[i][0]);
            item.put("product", rawData[i][1]);
            data[i - 1] = item;
        }
        response.put("data", data);

        return response;
    }
}
