package org.swu.vehiclecloud.mapper;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.*;

import java.util.List;
import java.util.Map;
import java.time.LocalDateTime;

/**
 * Excel数据映射接口
 */
@Mapper
public interface ExcelMapper {
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

    @Select("SELECT COUNT(*) > 0 FROM ${tableName} WHERE vehicleId = #{vehicleId} AND timestamp = #{timestamp}")
    int checkRecordExists(
        @Param("tableName") String tableName,
        @Param("vehicleId") String vehicleId,
        @Param("timestamp") LocalDateTime timestamp);

    /**
     * 查询指定表的数据，仅按车辆ID筛选
     * @param tableName 表名
     * @param vehicleId 车辆ID
     * @return 包含表数据的Map列表
     */
    @Select("SELECT * FROM ${tableName} WHERE vehicleId = #{vehicleId}")
    List<Map<String, Object>> selectByVehicleId(
            @Param("tableName") String tableName,
            @Param("vehicleId") String vehicleId);

    /**
     * 查询表是否存在特定车辆ID的记录（不限时间）
     * @param tableName 表名
     * @param vehicleId 车辆ID
     * @return 存在返回1，不存在返回0
     */
    @Select("SELECT COUNT(*) > 0 FROM ${tableName} WHERE vehicleId = #{vehicleId}")
    int checkRecordExistsByVehicle(
            @Param("tableName") String tableName,
            @Param("vehicleId") String vehicleId);

}