package org.swu.vehiclecloud.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.util.Date;

@Data
@TableName("activity_alert")
public class ActivityAlert {
    @TableId(type = IdType.AUTO)
    private Long id;

    @TableField("vehicle_id")
    private String vehicleId;

    @TableField("no_data_alert")
    private Boolean noDataAlert;

    @TableField("low_speed_alert")
    private Boolean lowSpeedAlert;

    @TableField("alert_level")
    private Integer alertLevel; // 1-警告 2-严重

    @TableField(value = "timestamp", fill = FieldFill.INSERT)
    private Date timestamp;

    @Version
    private Integer version; // 乐观锁版本号

    @TableLogic
    private Boolean deleted; // 逻辑删除标志
}
