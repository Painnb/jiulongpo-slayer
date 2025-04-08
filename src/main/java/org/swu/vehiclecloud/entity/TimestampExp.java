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
@TableName("timestamp_exp")
public class TimestampExp {
    @TableId
    private Integer id; // 自增id

    private String vehicleId; // 车辆id

    private Date timestampGNSS; // GNSS时间戳

    private Date timestamp3; // 文档没写这是什么

    private Date timestamp4; // 文档没写这是什么

    private Date timestamp; // 时间戳

    public TimestampExp(String vehicleId, Date timestampGNSS,
                        Date timestamp3, Date timestamp4,
                        Date timestamp) {
        this.vehicleId = vehicleId;
        this.timestampGNSS = timestampGNSS;
        this.timestamp3 = timestamp3;
        this.timestamp4 = timestamp4;
        this.timestamp = timestamp;
    }
}
