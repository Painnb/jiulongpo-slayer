package org.swu.vehiclecloud.mapper;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;

import java.util.List;
import java.util.Map;

@Mapper
public interface DataMapper {

    /**
     * 统计方向盘异常记录数
     * @return steering_exp表的记录总数
     */
    @Select("SELECT COUNT(*) FROM steering_exp")
    int countSteeringAnomalies();

    /**
     * 统计车速异常记录数
     * @return speed_exp表的记录总数
     */
    @Select("SELECT COUNT(*) FROM speed_exp")
    int countSpeedAnomalies();

    /**
     * 统计加速度异常记录数
     * @return acceleration_exp表的记录总数
     */
    @Select("SELECT COUNT(*) FROM acceleration_exp")
    int countAccelerationAnomalies();

    /**
     * 统计油门异常记录数（刹车系统）
     * @return brake_exp表的记录总数
     */
    @Select("SELECT COUNT(*) FROM brake_exp")
    int countBrakeAnomalies();

    /**
     * 统计发动机异常记录数
     * @return engine_exp表的记录总数
     */
    @Select("SELECT COUNT(*) FROM engine_exp")
    int countEngineAnomalies();
    /**
     * 统计地理位置异常记录数
     * @return geo_location_exp表的记录总数
     */
    @Select("SELECT COUNT(*) FROM geo_location_exp")
    int countGeolocationAnomalies();
    /**
     * 统计时间戳异常记录数
     * @return timestamp_exp表的记录总数
     */
    @Select("SELECT COUNT(*) FROM timestamp_exp")
    int countTimestampAnomalies();
    /**
     * 统计各车辆的总异常数量（包含所有7个异常表）
     * @return 包含vehicleId和count的Map列表
     */
    @Select("SELECT vehicleId as name, COUNT(*) as value FROM (" +
            "SELECT vehicleId FROM steering_exp " +
            "UNION ALL SELECT vehicleId FROM speed_exp " +
            "UNION ALL SELECT vehicleId FROM acceleration_exp " +
            "UNION ALL SELECT vehicleId FROM brake_exp " +
            "UNION ALL SELECT vehicleId FROM engine_exp " +
            "UNION ALL SELECT vehicleId FROM geo_location_exp " +
            "UNION ALL SELECT vehicleId FROM timestamp_exp" +
            ") t GROUP BY vehicleId")
    List<Map<String, Object>> countExceptionsByVehicle();

}
