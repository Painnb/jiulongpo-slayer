package org.swu.vehiclecloud.event;

import org.springframework.context.ApplicationEvent;
import java.util.Map;

public class MqttMessageEvent extends ApplicationEvent {
    private String topic;
    private Map<String, Object> payload;
    private String message;

    public MqttMessageEvent(Object source, String topic, Map<String, Object> payload) {
        super(source);
        this.topic = topic;
        this.payload = payload;
    }

    /*
    public MqttMessageEvent(Object source, String topic, String message) {
        super(source);
        this.topic = topic;
        this.message = message;
    }
     */

    public String getTopic() {
        return topic;
    }

    public Map<String, Object> getMessage() {
        return payload;
    }

    public String getMessageAsString() {
        return payload.toString();
    }

    // 添加获取JSON格式的方法
    public String getPayloadAsJson() {
        return null;
    }
}
// 发布数据示例：
// {
//     "header": {
//         "prefix": 242,
//         "dataLen": 82,
//         "dataCategory": 21,
//         "ver": 1,
//         "timestamp": 1723588671557,
//         "ctl": 0
//     },
//     "body": {
//         "vehicleId": "QD1E0035",
//         "messageId": 55822,
//         "timestampGNSS": 1723588671557,
//         "velocityGNSS": 66.0,车辆速度
//         "position": {
//             "longitude": 106.3447676,
//             "latitude": 29.498806099999996,
//             "elevation": 2947
//         },
//         "heading": 277.9499,
//         "tapPos": 0,
//         "steeringAngle": 0,
//         "engineTorque": 0,
//         "destLocation": {
//             "longitude": -180.0,
//             "latitude": -90.0
//         },
//         "passPointsNum": 0
//     }
// }
