package org.swu.vehiclecloud.mapper;

import org.apache.ibatis.annotations.Mapper;

import java.util.List;
import java.util.Map;

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
    List<Map<String, Object>> selectAllFromTable(String tableName);
}