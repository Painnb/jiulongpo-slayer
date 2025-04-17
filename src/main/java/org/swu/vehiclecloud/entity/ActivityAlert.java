
package org.swu.vehiclecloud.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.*;

import java.sql.Timestamp;

@Getter
@Setter
@ToString
@NoArgsConstructor
@TableName("activity_alert")
public class ActivityAlert {
    @TableId
    private Integer id;

    @TableField("vehicle_id")
    private String vehicleId;

    @TableField("no_data_alert")
    private Boolean noDataAlert;

    @TableField("low_speed_alert")
    private Boolean lowSpeedAlert;

    @TableField("timestamp")
    private Timestamp timestamp;

    public ActivityAlert(String vehicleId, Boolean noDataAlert, Boolean lowSpeedAlert, Timestamp timestamp) {
        this.vehicleId = vehicleId;
        this.noDataAlert = noDataAlert;
        this.lowSpeedAlert = lowSpeedAlert;
        this.timestamp = timestamp;
    }
}