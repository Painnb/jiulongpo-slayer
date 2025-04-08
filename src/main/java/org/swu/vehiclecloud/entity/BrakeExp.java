package org.swu.vehiclecloud.entity;

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
@TableName("brake_exp")
public class BrakeExp {
    @TableId
    private Integer id; // 自增id

    private String vehicleId; // 车辆id

    private boolean brakeFlag; // 制动踏板开关

    private int brakePos; // 制动踏板开度

    private int brakePressure; // 制动主缸压力

    private Date timestamp; // 时间戳

    public BrakeExp(String vehicleId, boolean brakeFlag,
                    int brakePos, int brakePressure,
                    Date timestamp) {
        this.vehicleId = vehicleId;
        this.brakeFlag = brakeFlag;
        this.brakePos = brakePos;
        this.brakePressure = brakePressure;
        this.timestamp = timestamp;
    }
}
