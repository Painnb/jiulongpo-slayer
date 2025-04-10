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
@TableName("speed_exp")
public class SpeedExp {
    @TableId
    private Integer id; // 自增id

    private String vehicleId; // 车辆id

    private double velocityGNSS; // GNSS速度

    private double velocityCAN; // 当前车速

    private Timestamp timestamp; // 时间戳

    public SpeedExp(String vehicleId, double velocityGNSS,
                           double velocityCAN, Timestamp timestamp) {
        this.vehicleId = vehicleId;
        this.velocityGNSS = velocityGNSS;
        this.velocityCAN = velocityCAN;
        this.timestamp = timestamp;
    }
}
