package org.swu.vehiclecloud.entity;

import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

@Getter
@Setter
@ToString
@NoArgsConstructor
@TableName("mqtt_data")
public class MqttData {
    @TableId
    private Integer id; // 自增id

    private String data; // mqtt数据

    public MqttData(String data) {
        this.data = data;
    }
}
