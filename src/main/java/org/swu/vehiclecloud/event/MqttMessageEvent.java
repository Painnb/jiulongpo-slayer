package org.swu.vehiclecloud.event;

import org.springframework.context.ApplicationEvent;

public class MqttMessageEvent extends ApplicationEvent {
    private String topic;
    private String message;

    public MqttMessageEvent(Object source, String topic, String message) {
        super(source);
        this.topic = topic;
        this.message = message;
    }

    public String getTopic() {
        return topic;
    }

    public String getMessage() {
        return message;
    }

    // 添加获取JSON格式的方法
    public String getPayloadAsJson() {
        return null;
    }
}
