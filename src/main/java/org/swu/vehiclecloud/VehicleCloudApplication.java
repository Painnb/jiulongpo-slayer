package org.swu.vehiclecloud;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.web.servlet.ServletComponentScan;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.transaction.annotation.EnableTransactionManagement;
import org.swu.vehiclecloud.config.MqttConfigProperties;
import org.springframework.boot.CommandLineRunner;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;

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
@EnableTransactionManagement
@Component
public class VehicleCloudApplication implements CommandLineRunner {

    /**
     * 应用程序主入口
     * @param args 命令行参数
     */
    public static void main(String[] args) {
        SpringApplication.run(VehicleCloudApplication.class, args);
    }

    @Override
    @Async
    public void run(String... args) throws Exception {
        try {
            ProcessBuilder pb = new ProcessBuilder("python", "pythonMLAnomaly/main.py");
            pb.redirectErrorStream(true);
            Process process = pb.start();

            BufferedReader reader = new BufferedReader(
                new InputStreamReader(process.getInputStream()));
            String line;
            while ((line = reader.readLine()) != null) {
                System.out.println("Python输出: " + line);
            }
        } catch (IOException e) {
            System.err.println("启动Python进程失败: " + e.getMessage());
        }
    }
}
    