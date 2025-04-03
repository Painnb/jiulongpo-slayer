package org.swu.vehiclecloud;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.swu.vehiclecloud.config.MqttConfigProperties;

@SpringBootApplication
@MapperScan("org.swu.vehiclecloud.mapper")
@EnableConfigurationProperties(MqttConfigProperties.class)
public class VehicleCloudApplication {

    public static void main(String[] args) {
        SpringApplication.run(VehicleCloudApplication.class, args);
    }

}
