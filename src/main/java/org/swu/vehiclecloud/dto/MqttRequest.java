package org.swu.vehiclecloud.dto;

import lombok.Data;

import java.util.List;

@Data
public class MqttRequest {
    private String brokerUrl;
    private String clientId;
    private String username;
    private String password;
    private List<String> subTopics;

    public MqttRequest() {}
}
