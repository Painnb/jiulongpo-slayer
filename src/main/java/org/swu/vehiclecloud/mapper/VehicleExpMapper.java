package org.swu.vehiclecloud.mapper;

import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Options;
import org.swu.vehiclecloud.entity.AccelerationExp;
import org.swu.vehiclecloud.entity.EngineExp;
import org.swu.vehiclecloud.entity.SpeedExp;

@Mapper
public interface VehicleExpMapper {
    /**
     * 插入加速度异常对象
     * @param accelerationExp 加速度异常对象
     */
    @Insert("INSERT INTO acceleration_exp (vehicleId, accelerationLon, accelerationLat, accelerationVer, timestamp) " +
            "VALUES (#{vehicleId}, #{accelerationLon}, #{accelerationLat}, #{accelerationVer}, #{timestamp}")
    @Options(useGeneratedKeys = true, keyProperty = "id")
    void insert(AccelerationExp accelerationExp);

    /**
     * 插入速度异常对象
     * @param speedExp 速度异常对象
     */
    @Insert("INSERT INTO speed_exp (vehicleId, velocityGNSS, velocityCAN, timestamp) " +
            "VALUES (#{vehicleId}, #{velocityGNSS}, #{velocityCAN}, #{timestamp}")
    @Options(useGeneratedKeys = true, keyProperty = "id")
    void insert(SpeedExp speedExp);

    /**
     * 插入发动机异常对象
     * @param engineExp 发动机异常对象
     */
    @Insert("INSERT INTO engine_exp (vehicleId, engineSpeed, engineTorque, timestamp) " +
            "VALUES (#{vehicleId}, #{engineSpeed}, #{engineTorque}, #{timestamp}")
    @Options(useGeneratedKeys = true, keyProperty = "id")
    void insert(EngineExp engineExp);
}
