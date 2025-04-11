package org.swu.vehiclecloud;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.web.servlet.ServletComponentScan;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.swu.vehiclecloud.config.MqttConfigProperties;

/**
 * 车辆云平台主启动类
 * <p>
 * 使用Spring Boot框架构建的应用程序入口
 * @SpringBootApplication 组合注解，包含@Configuration, @EnableAutoConfiguration, @ComponentScan
 * @ServletComponentScan 启用Servlet组件扫描
 * @MapperScan 指定MyBatis mapper接口扫描路径
 * @EnableConfigurationProperties 启用配置属性绑定，用于MQTT配置
 */
@SpringBootApplication
@ServletComponentScan
@MapperScan("org.swu.vehiclecloud.mapper")
@EnableConfigurationProperties(MqttConfigProperties.class)
public class VehicleCloudApplication {

    /**
     * 应用程序主入口
     * @param args 命令行参数
     */
    public static void main(String[] args) {
        SpringApplication.run(VehicleCloudApplication.class, args);
    }

}
