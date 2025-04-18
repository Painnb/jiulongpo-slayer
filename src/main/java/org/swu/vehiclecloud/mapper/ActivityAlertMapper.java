package org.swu.vehiclecloud.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Options;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.springframework.stereotype.Repository;
import org.swu.vehiclecloud.entity.ActivityAlert;

import java.util.Date;
import java.util.List;

@Repository
public interface ActivityAlertMapper extends BaseMapper<ActivityAlert> {
    /**
     * 插入车辆活跃度对象
     * @param activityAlert 车辆活跃度对象
     */
    @Insert("INSERT INTO activity_alert (vehicle_id, no_data_alert, low_speed_alert, timestamp) " +
            "VALUES (#{vehicleId}, #{no_data_alert}, #{low_speed_alert}, #{timestamp})")
    @Options(useGeneratedKeys = true, keyProperty = "id")
    void insertActivityAlert(ActivityAlert activityAlert);

    /**
     * 获取指定车辆的最新告警记录
     */
    @Select("SELECT id, vehicle_id as vehicleId, alert_type as alertType, " +
            "no_data_alert as noDataAlert, low_speed_alert as lowSpeedAlert, " +
            "timestamp as alertTime FROM activity_alert " +
            "WHERE vehicle_id = #{vehicleId} ORDER BY timestamp DESC LIMIT 1")
    ActivityAlert getLatestAlertForVehicle(String vehicleId);

    /**
     * 查询指定时间范围内的统计数据
     * @param startTime 开始时间
     * @param endTime 结束时间
     * @return 统计数据列表
     */
    @Select("SELECT id, vehicle_id as vehicleId, alert_type as alertType, " +
            "no_data_alert as noDataAlert, low_speed_alert as lowSpeedAlert, " +
            "timestamp as alertTime FROM activity_alert " +
            "WHERE alert_type = 'STATS' " +
            "AND timestamp BETWEEN #{startTime} AND #{endTime} " +
            "ORDER BY timestamp ASC")
    List<ActivityAlert> findStatsByTimeRange(@Param("startTime") Date startTime, @Param("endTime") Date endTime);

    /**
     * 查询最新的统计数据
     * @return 最新统计数据
     */
    @Select("SELECT id, vehicle_id as vehicleId, alert_type as alertType, " +
            "no_data_alert as noDataAlert, low_speed_alert as lowSpeedAlert, " +
            "timestamp as alertTime FROM activity_alert " +
            "WHERE alert_type = 'STATS' ORDER BY timestamp DESC LIMIT 1")
    ActivityAlert findLatestStats();

    /**
     * 查询指定时间范围内的在线车辆数（有数据上报的车辆）
     * @param startTime 开始时间
     * @param endTime 结束时间
     * @return 在线车辆数
     */
    @Select("SELECT COUNT(DISTINCT vehicle_id) FROM activity_alert " +
            "WHERE timestamp BETWEEN #{startTime} AND #{endTime} " +
            "AND vehicle_id IS NOT NULL AND vehicle_id != '' " +
            "AND vehicle_id != 'system'")
    int countOnlineVehicles(@Param("startTime") Date startTime, @Param("endTime") Date endTime);

    /**
     * 查询指定时间范围内的活跃车辆数（非低速告警的车辆）
     * @param startTime 开始时间
     * @param endTime 结束时间
     * @return 活跃车辆数
     */
    @Select("SELECT COUNT(DISTINCT vehicle_id) FROM activity_alert " +
            "WHERE timestamp BETWEEN #{startTime} AND #{endTime} " +
            "AND (alert_type = 'ACTIVITY' OR low_speed_alert = false) " +
            "AND vehicle_id IS NOT NULL AND vehicle_id != '' " +
            "AND vehicle_id != 'system'")
    int countActiveVehicles(@Param("startTime") Date startTime, @Param("endTime") Date endTime);

    /**
     * 获取指定车辆的在线时长
     * @param vehicleId 车辆ID
     * @param startTime 开始时间
     * @param endTime 结束时间
     * @return 在线时长（毫秒）
     */
    @Select("SELECT COUNT(*) * 10000 FROM activity_alert " +
            "WHERE vehicle_id = #{vehicleId} " +
            "AND timestamp BETWEEN #{startTime} AND #{endTime}")
    long getVehicleOnlineTime(@Param("vehicleId") String vehicleId,
                              @Param("startTime") Date startTime,
                              @Param("endTime") Date endTime);

    /**
     * 获取指定时间范围内所有车辆ID
     * @param startTime 开始时间
     * @param endTime 结束时间
     * @return 车辆ID列表
     */
    @Select("SELECT DISTINCT vehicle_id FROM activity_alert " +
            "WHERE timestamp BETWEEN #{startTime} AND #{endTime} " +
            "AND vehicle_id IS NOT NULL AND vehicle_id != ''")
    List<String> getAllVehicleIds(@Param("startTime") Date startTime, @Param("endTime") Date endTime);
}