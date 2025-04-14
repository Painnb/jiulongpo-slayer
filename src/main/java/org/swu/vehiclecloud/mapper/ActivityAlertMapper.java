package org.swu.vehiclecloud.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.*;
import org.swu.vehiclecloud.entity.ActivityAlert;
import java.util.List;

@Mapper
public interface ActivityAlertMapper extends BaseMapper<ActivityAlert> {

    @Insert("<script>" +
            "INSERT INTO activity_alert (vehicle_id, no_data_alert, low_speed_alert, alert_level, timestamp) " +
            "VALUES <foreach collection='list' item='item' separator=','>" +
            "(#{item.vehicleId}, #{item.noDataAlert}, #{item.lowSpeedAlert}, #{item.alertLevel}, #{item.timestamp})" +
            "</foreach>" +
            "</script>")
    int batchInsert(@Param("list") List<ActivityAlert> alerts);

    @Select("SELECT * FROM activity_alert WHERE vehicle_id = #{vehicleId} ORDER BY timestamp DESC LIMIT 100")
    List<ActivityAlert> selectRecentByVehicle(@Param("vehicleId") String vehicleId);
}
