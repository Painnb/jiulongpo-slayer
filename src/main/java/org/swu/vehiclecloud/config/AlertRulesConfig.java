package org.swu.vehiclecloud.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Data
@Configuration
@ConfigurationProperties(prefix = "alert.rules")
public class AlertRulesConfig {
    // 无数据报警阈值(秒)
    private int noDataThreshold = 15;

    // 低速报警阈值(m/s)
    private double lowSpeedThreshold = 1.0;

    // 严重异常阈值(m/s)
    private double criticalThreshold = 0.5;
}