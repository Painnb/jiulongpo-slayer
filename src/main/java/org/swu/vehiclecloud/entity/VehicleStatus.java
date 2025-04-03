package org.swu.vehiclecloud.entity;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;
import java.util.Date;

@Getter
@Setter
@ToString
@NoArgsConstructor
@TableName("vehicle_status")
public class VehicleStatus {

    @TableId
    private Long id; // 主键ID
    
    private Date timestamp; // 时间戳
    
    private Integer longitude; // 经度，单位：百万分之一度
    
    private Integer latitude; // 纬度，单位：百万分之一度
    
    private String vehicleModel; // 车辆型号
    
    private Integer speed; // 车速，单位：0.01km/h
    
    private Integer lateralAcceleration; // 横向加速度，单位：0.01m/s²
    
    private Integer longitudinalAcceleration; // 纵向加速度，单位：0.01m/s²
    
    private Integer verticalAcceleration; // 垂向加速度，单位：0.01m/s²
    
    private Integer steeringAngle; // 方向盘转角，单位：0.01度
    
    private Integer recommendedSpeed; // 建议车速，单位：0.01km/h
    
    private Integer tirePressure; // 胎压，单位：0.1kPa
    
    private Integer batteryLevel; // 电量，单位：0.1%
}