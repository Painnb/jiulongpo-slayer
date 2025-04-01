package org.swu.vehiclecloud.mapper;

import org.apache.ibatis.annotations.Mapper;
import java.util.List;
import java.util.Map;

@Mapper
public interface ExcelMapper {
    List<Map<String, Object>> selectAllFromTable(String tableName);
}