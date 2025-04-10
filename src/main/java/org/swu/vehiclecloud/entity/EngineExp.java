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
@TableName("engine_exp")
public class EngineExp {
    @TableId
    private Integer id; // 自增id

    private String vehicleId; // 车辆id

    private double engineSpeed; // 发动机转速

    private double engineTorque; // 发动机扭矩

    private Timestamp timestamp; // 时间戳

    public EngineExp(String vehicleId, double engineSpeed, double engineTorque, Timestamp timestamp) {
        this.vehicleId = vehicleId;
        this.engineSpeed = engineSpeed;
        this.engineTorque = engineTorque;
        this.timestamp = timestamp;
    }
}
