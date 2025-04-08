package org.swu.vehiclecloud.mapper;

import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Options;
import org.swu.vehiclecloud.entity.*;

@Mapper
public interface VehicleExpMapper {
    /**
     * 插入加速度异常对象
     * @param accelerationExp 加速度异常对象
     */
    @Insert("INSERT INTO acceleration_exp (vehicleId, accelerationLon, accelerationLat, accelerationVer, timestamp) " +
            "VALUES (#{vehicleId}, #{accelerationLon}, #{accelerationLat}, #{accelerationVer}, #{timestamp})")
    @Options(useGeneratedKeys = true, keyProperty = "id")
    void insert(AccelerationExp accelerationExp);

    /**
     * 插入速度异常对象
     * @param speedExp 速度异常对象
     */
    @Insert("INSERT INTO speed_exp (vehicleId, velocityGNSS, velocityCAN, timestamp) " +
            "VALUES (#{vehicleId}, #{velocityGNSS}, #{velocityCAN}, #{timestamp})")
    @Options(useGeneratedKeys = true, keyProperty = "id")
    void insert(SpeedExp speedExp);

    /**
     * 插入发动机异常对象
     * @param engineExp 发动机异常对象
     */
    @Insert("INSERT INTO engine_exp (vehicleId, engineSpeed, engineTorque, timestamp) " +
            "VALUES (#{vehicleId}, #{engineSpeed}, #{engineTorque}, #{timestamp})")
    @Options(useGeneratedKeys = true, keyProperty = "id")
    void insert(EngineExp engineExp);

    /**
     * 插入制动异常对象
     * @param brakeExp 制动异常对象
     */
    @Insert("INSERT INTO brake_exp (vehicleId, brakeFlag, brakePos, brakePressure, timestamp) " +
            "VALUES (#{vehicleId}, #{brakeFlag}, #{brakePos}, #{brakePressure}, #{timestamp})")
    @Options(useGeneratedKeys = true, keyProperty = "id")
    void insert(BrakeExp brakeExp);

    /**
     * 插入转向异常对象
     * @param steeringExp 转向异常对象
     */
    @Insert("INSERT INTO steering_exp (vehicleId, steeringAngle, yawRate, timestamp) " +
            "VALUES (#{vehicleId}, #{steeringAngle}, #{yawRate}, #{timestamp})")
    @Options(useGeneratedKeys = true, keyProperty = "id")
    void insert(SteeringExp steeringExp);

    /**
     * 插入时间戳异常对象
     * @param timestampExp 时间戳异常对象
     */
    @Insert("INSERT INTO timestamp_exp (vehicleId, timestampGNSS, timestamp3, timestamp4, timestamp) " +
            "VALUES (#{vehicleId}, #{timestampGNSS}, #{timestamp3}, #{timestamp4}, #{timestamp})")
    @Options(useGeneratedKeys = true, keyProperty = "id")
    void insert(TimestampExp timestampExp);

    /**
     * 插入经纬度异常对象
     * @param geoLocationExp 经纬度异常对象
     */
    @Insert("INSERT INTO geo_location_exp (vehicleId, longitude, latitude, timestamp) " +
            "VALUES (#{vehicleId}, #{longitude}, #{latitude}, #{timestamp})")
    @Options(useGeneratedKeys = true, keyProperty = "id")
    void insert(GeoLocationExp geoLocationExp);
}
