package org.swu.vehiclecloud.expshow;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import java.util.List;
import java.util.ArrayList;
import java.util.Map;
import java.util.HashMap;

@RestController
@RequestMapping("/api/exception-stats")
public class ExceptionStatsController {

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @GetMapping
    public List<Map<String, Object>> getExceptionStats() {
        List<Map<String, Object>> result = new ArrayList<>();

        // 1. 加速度异常表
        int accelerationCount = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM acceleration_exp", Integer.class);
        result.add(createStatItem(accelerationCount, "加速度异常"));

        // 2. 刹车异常表
        int brakeCount = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM brake_exp", Integer.class);
        result.add(createStatItem(brakeCount, "刹车异常"));

        // 3. 发动机异常表
        int engineCount = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM engine_exp", Integer.class);
        result.add(createStatItem(engineCount, "发动机异常"));

        // 4. 地理位置异常表
        int geoLocationCount = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM geo_location_exp", Integer.class);
        result.add(createStatItem(geoLocationCount, "地理位置异常"));

        // 5. 速度异常表
        int speedCount = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM speed_exp", Integer.class);
        result.add(createStatItem(speedCount, "速度异常"));

        // 6. 转向异常表
        int steeringCount = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM steering_exp", Integer.class);
        result.add(createStatItem(steeringCount, "转向异常"));

        // 7. 时间戳异常表
        int timestampCount = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM timestamp_exp", Integer.class);
        result.add(createStatItem(timestampCount, "时间戳异常"));

        return result;
    }

    private Map<String, Object> createStatItem(int value, String name) {
        Map<String, Object> item = new HashMap<>();
        item.put("value", value);
        item.put("name", name);
        return item;
    }
}

