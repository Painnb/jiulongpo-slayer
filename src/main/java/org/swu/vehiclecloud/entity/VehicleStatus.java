package org.swu.vehiclecloud.entity;


import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;
import java.util.Date;

/**
 * 车辆状态实体类
 * 对应数据库中的vehicle_status表
 */
@Getter
@Setter
@ToString
@NoArgsConstructor
@TableName("vehicle_status")
public class VehicleStatus {

        /**
     * 主键ID
     */
    @TableId
    private Long id;
    
        /**
     * 时间戳
     */
    private Date timestamp;
    
        /**
     * 经度，单位：百万分之一度
     */
    private Integer longitude;
    
        /**
     * 纬度，单位：百万分之一度
     */
    private Integer latitude;
    
        /**
     * 车辆型号
     */
    private String vehicleModel;
    
        /**
     * 车速，单位：0.01km/h
     */
    private Integer speed;
    
        /**
     * 横向加速度，单位：0.01m/s²
     */
    private Integer lateralAcceleration;
    
        /**
     * 纵向加速度，单位：0.01m/s²
     */
    private Integer longitudinalAcceleration;
    
        /**
     * 垂向加速度，单位：0.01m/s²
     */
    private Integer verticalAcceleration;
    
        /**
     * 方向盘转角，单位：0.01度
     */
    private Integer steeringAngle;
    
        /**
     * 建议车速，单位：0.01km/h
     */
    private Integer recommendedSpeed;
    
        /**
     * 胎压，单位：0.1kPa
     */
    private Integer tirePressure;
    
        /**
     * 电量，单位：0.1%
     */
    private Integer batteryLevel;
}