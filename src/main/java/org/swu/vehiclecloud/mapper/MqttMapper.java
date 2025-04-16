package org.swu.vehiclecloud.mapper;

import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Options;
import org.swu.vehiclecloud.entity.MqttData;

@Mapper
public interface MqttMapper {
    /**
     * 插入mqtt数据对象
     * @param mqttData mqtt数据对象
     */
    @Insert("INSERT INTO mqtt_data (data) " +
            "VALUES (#{data})")
    @Options(useGeneratedKeys = true, keyProperty = "id")
    void insert(MqttData mqttData);
}
