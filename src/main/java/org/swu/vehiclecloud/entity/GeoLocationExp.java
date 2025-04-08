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
@TableName("geo_location_exp")
public class GeoLocationExp {
    @TableId
    private Integer id; // 自增id

    private String vehicleId; // 车辆id

    private double longitude; // 经度

    private double latitude; // 纬度

    private Date timestamp; // 时间戳

    public GeoLocationExp(String vehicleId, double longitude, double latitude, Date timestamp) {
        this.vehicleId = vehicleId;
        this.longitude = longitude;
        this.latitude = latitude;
        this.timestamp = timestamp;
    }
}
