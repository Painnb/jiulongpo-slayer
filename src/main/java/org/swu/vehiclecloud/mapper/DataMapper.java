package org.swu.vehiclecloud.mapper;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.swu.vehiclecloud.entity.MlExpcetion;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

// DataMapper.java
// DataMapper.java
@Mapper
public interface DataMapper {
    /**
     * 查询异常表记录数量
     * @param tableName 异常表名
     * @return 记录数量
     */
    @Select("SELECT COUNT(*) FROM ${tableName}")
    int countExceptionRecords(@Param("tableName") String tableName);

    /**
     * 查询异常表数据（带时间范围筛选）
     * @param tableName 异常表名
     * @param startTime 开始时间
     * @param endTime 结束时间
     * @return 异常数据列表
     */
    @Select("SELECT * FROM ${tableName} WHERE timestamp BETWEEN #{startTime} AND #{endTime}")
    List<Map<String, Object>> selectExceptionDataWithTimeRange(
            @Param("tableName") String tableName,
            @Param("startTime") LocalDateTime startTime,
            @Param("endTime") LocalDateTime endTime);

    /**
     * 查询异常表数据（带车辆ID和时间范围筛选）
     * @param tableName 异常表名
     * @param vehicleId 车辆ID
     * @param startTime 开始时间
     * @param endTime 结束时间
     * @return 异常数据列表
     */
    @Select("SELECT * FROM ${tableName} WHERE vehicleId = #{vehicleId} AND timestamp BETWEEN #{startTime} AND #{endTime}")
    List<Map<String, Object>> selectExceptionDataWithFilter(
            @Param("tableName") String tableName,
            @Param("vehicleId") String vehicleId,
            @Param("startTime") LocalDateTime startTime,
            @Param("endTime") LocalDateTime endTime);

    /**
     * 查询所有异常表名
     * @return 异常表名列表
     */
    @Select("SELECT table_name FROM information_schema.tables " +
            "WHERE table_schema = DATABASE() AND table_name LIKE '%_exp'")
    List<String> listExceptionTables();
  
  
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

    /**
     * 获取机器学习异常数量统计
     * @return 机器学习检测的车辆异常数量统计列表
     */
    @Select("SELECT t1.* " +
            "FROM ml_exp t1 " +
            "JOIN ( " +
            "    SELECT vehicleId, MAX(mse) AS max_mse " +
            "    FROM ml_exp " +
            "    GROUP BY vehicleId " +
            ") t2 " +
            "ON t1.vehicleId = t2.vehicleId " +
            "AND t1.mse = t2.max_mse")
    List<MlExpcetion> selectMlExceptionData();
}
