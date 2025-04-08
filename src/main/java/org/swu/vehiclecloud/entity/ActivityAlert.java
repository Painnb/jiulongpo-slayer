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
@TableName("activity_alert")
public class ActivityAlert {
    @TableId
    private Long id; // 自增主键
    
    private String vehicleId; // 车辆ID
    
    private boolean noDataAlert; // 无数据异常，1表示异常，0表示正常
    
    private boolean lowSpeedAlert; // 低速异常，1表示异常，0表示正常
    
    private Date timestamp; // 时间戳
} 