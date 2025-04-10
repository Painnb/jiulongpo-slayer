package org.swu.vehiclecloud.dto;

import lombok.Data;

import java.util.List;

/**
 * MQTT连接请求数据传输对象
 */
@Data
public class MqttRequest {
    /**
     * MQTT代理服务器地址
     */
    private String brokerUrl;
    
    /**
     * 客户端ID
     */
    private String clientId;
    
    /**
     * 连接用户名
     */
    private String username;
    
    /**
     * 连接密码
     */
    private String password;
    
    /**
     * 订阅的主题列表
     */
    private List<String> subTopics;

    /**
     * 默认构造函数
     */
    public MqttRequest() {}
}
