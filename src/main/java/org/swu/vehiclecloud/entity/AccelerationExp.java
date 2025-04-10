package org.swu.vehiclecloud.entity;

import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

import java.sql.Timestamp;
import java.util.Date;

@Getter
@Setter
@ToString
@NoArgsConstructor
@TableName("acceleration_exp")
public class AccelerationExp {
    @TableId
    private Integer id; // 自增id

    private String vehicleId; // 车辆id

    private double accelerationLon; // 纵向加速度

    private double accelerationLat; // 横向加速度

    private double accelerationVer; // 垂向加速度

    private Timestamp timestamp; // 时间戳

    public AccelerationExp(String vehicleId, double accelerationLon,
                           double accelerationLat, double accelerationVer,
                           Timestamp timestamp) {
        this.vehicleId = vehicleId;
        this.accelerationLon = accelerationLon;
        this.accelerationLat = accelerationLat;
        this.accelerationVer = accelerationVer;
        this.timestamp = timestamp;
    }
}
