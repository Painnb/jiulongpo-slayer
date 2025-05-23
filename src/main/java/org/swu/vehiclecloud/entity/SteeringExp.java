package org.swu.vehiclecloud.entity;

import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

import java.sql.Timestamp;

@Getter
@Setter
@ToString
@NoArgsConstructor
@TableName("steering_exp")
public class SteeringExp {
    @TableId
    private Integer id; // 自增id

    private String vehicleId; // 车辆id

    private double steeringAngle; // 方向盘转角

//    private double yawRate; // 横摆角速度

    private Timestamp timestamp; // 时间戳

    public SteeringExp(String vehicleId, double steeringAngle,
                       Timestamp timestamp) {
        this.vehicleId = vehicleId;
        this.steeringAngle = steeringAngle;
//        this.yawRate = yawRate;
        this.timestamp = timestamp;
    }
}
