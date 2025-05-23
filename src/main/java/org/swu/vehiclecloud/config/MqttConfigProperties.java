package org.swu.vehiclecloud.config;

import lombok.Data;
import org.eclipse.paho.client.mqttv3.MqttConnectOptions;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.integration.mqtt.core.DefaultMqttPahoClientFactory;
import org.springframework.integration.mqtt.core.MqttPahoClientFactory;

import java.util.List;
import java.util.regex.Pattern;

@Data
@ConfigurationProperties(prefix = "spring.mqtt")
public class MqttConfigProperties {
    private String brokerUrl;
    private String clientId = generateRandomClientId();
    private String username;
    private String password;
    private List<String> subTopics;
    private int defaultQos = 0;

    private String generateRandomClientId() {
        return "client-" + System.currentTimeMillis() + "-" + (int)(Math.random() * 1000);
    }

    @Bean
    public MqttPahoClientFactory mqttClientFactory() {
        DefaultMqttPahoClientFactory mqttClientFactory = new DefaultMqttPahoClientFactory();

        MqttConnectOptions options = new MqttConnectOptions();
        options.setCleanSession(true);
        options.setUserName(username);
        options.setPassword(password.toCharArray());
        options.setServerURIs(new String[]{brokerUrl});
        mqttClientFactory.setConnectionOptions(options);

        return mqttClientFactory;
    }
}
