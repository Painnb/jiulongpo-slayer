package org.swu.vehiclecloud.config;

import lombok.Data;
import org.eclipse.paho.client.mqttv3.MqttConnectOptions;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.integration.mqtt.core.DefaultMqttPahoClientFactory;
import org.springframework.integration.mqtt.core.MqttPahoClientFactory;

import java.util.List;

@Data
@ConfigurationProperties(prefix = "spring.mqtt")
public class MqttConfigProperties {
    private String brokerUrl;
    private String clientId;
    private String username;
    private String password;
    private List<String> subTopics;
    private int connectionTimeout = 30;
    private boolean enabled = true; // 新增启用开关

    @Bean
    public MqttPahoClientFactory mqttClientFactory() {
        if (!enabled) {  // 如果禁用则返回null
            return null;
        }
        DefaultMqttPahoClientFactory mqttPahoClientFactory = new DefaultMqttPahoClientFactory();
        MqttConnectOptions options = new MqttConnectOptions();
        options.setCleanSession(true);
        options.setUserName(username);
        options.setPassword(password.toCharArray());
        options.setServerURIs(new String[]{brokerUrl});
        mqttPahoClientFactory.setConnectionOptions(options);

        return mqttPahoClientFactory;
    }
}
