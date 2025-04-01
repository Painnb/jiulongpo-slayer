package org.swu.vehiclecloud.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;
import java.util.List;

@Configuration
@ConfigurationProperties(prefix = "mqtt")
public class MqttConfigProperties {
    private String brokerUrl;
    private String clientId;
    private String username;
    private String password;
    private List<String> subscribeTopics;
    private int connectionTimeout = 30;

    // Getters and Setters (需 Lombok 或手动生成)
}
