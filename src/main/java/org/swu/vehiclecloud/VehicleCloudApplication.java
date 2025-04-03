package org.swu.vehiclecloud;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.web.servlet.ServletComponentScan;

@SpringBootApplication
@ServletComponentScan
@MapperScan("org.swu.vehiclecloud.mapper")
public class VehicleCloudApplication {

    public static void main(String[] args) {
        SpringApplication.run(VehicleCloudApplication.class, args);
    }

}
