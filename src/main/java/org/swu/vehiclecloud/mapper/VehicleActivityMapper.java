package org.swu.vehiclecloud.mapper;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@Mapper
public interface VehicleActivityMapper {

    /**
     * 查询所有车辆的活动数据（合并所有表）
     * @return 包含车辆ID和活动次数的列表
     */
    @Select("<script>" +
            "SELECT vehicleId, COUNT(*) as activity_count FROM (" +
            "  <foreach collection='tables' item='table' separator=' UNION ALL '>" +
            "    SELECT vehicleId FROM ${table} WHERE timestamp BETWEEN #{startTime} AND #{endTime}" +
            "  </foreach>" +
            ") AS combined_data " +
            "GROUP BY vehicleId " +
            "ORDER BY activity_count DESC" +
            "</script>")
    List<Map<String, Object>> selectAllVehicleActivities(@Param("tables") List<String> tables,@Param("startTime") LocalDateTime startTime,
                                                         @Param("endTime") LocalDateTime endTime);

    /**
     * 查询指定表的所有数据
     * @param tableName 要查询的表名
     * @return 包含表数据的Map列表
     */
    @Select("SELECT * FROM ${tableName}")
    List<Map<String, Object>> selectAllFromTable(@Param("tableName") String tableName);

    /**
     * 查询指定表的数据，按车辆ID和时间范围筛选
     * @param tableName 表名
     * @param vehicleId 车辆ID
     * @param startTime 开始时间
     * @param endTime 结束时间
     * @return 包含表数据的Map列表
     */
    @Select("SELECT * FROM ${tableName} WHERE vehicleId = #{vehicleId} AND timestamp BETWEEN #{startTime} AND #{endTime}")
    List<Map<String, Object>> selectFromTableWithFilter(
            @Param("tableName") String tableName,
            @Param("vehicleId") String vehicleId,
            @Param("startTime") LocalDateTime startTime,
            @Param("endTime") LocalDateTime endTime);

    /**
     * 查询表是否存在特定车辆ID和时间戳的记录
     * @param tableName 表名
     * @param vehicleId 车辆ID
     * @param timestamp 时间戳
     * @return 存在返回1，不存在返回0
     */
    @Select("SELECT COUNT(*) > 0 FROM ${tableName} WHERE vehicleId = #{vehicleId} AND timestamp = #{timestamp}")
    int checkRecordExists(
            @Param("tableName") String tableName,
            @Param("vehicleId") String vehicleId,
            @Param("timestamp") LocalDateTime timestamp);
}
