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
@TableName("ml_exp")
public class MlExpcetion {
    @TableId
    private Integer id; // 自增id

    private String vehicleId; // 车辆id

    private Timestamp timestamp; // 时间戳

    private double mse; // mean squared error 平均平方误差，大于0.1表示异常

    public MlExpcetion(String vehicleId, Timestamp timestamp, double mse) {
        this.vehicleId = vehicleId;
        this.timestamp = timestamp;
        this.mse = mse;
    }
}
