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

    public MqttMessageEvent(Object source, String topic, String message) {
        super(source);
        this.topic = topic;
        this.message = message;
    }

    public String getTopic() {
        return topic;
    }

    public Map<String, Object> getMessage() {
        return payload;
    }

    public String getMessageAsString() {
        return message;
    }

    // 添加获取JSON格式的方法
    public String getPayloadAsJson() {
        return null;
    }
}
