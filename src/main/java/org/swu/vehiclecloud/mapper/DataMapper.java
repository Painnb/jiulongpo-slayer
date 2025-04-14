package org.swu.vehiclecloud.mapper;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

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
}
