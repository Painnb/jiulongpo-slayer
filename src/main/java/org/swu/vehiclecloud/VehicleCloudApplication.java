package org.swu.vehiclecloud;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.web.servlet.ServletComponentScan;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.scheduling.annotation.EnableScheduling;
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
@EnableScheduling  // 启用定时任务
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
    /*
//                            _ooOoo_  
//                           o8888888o  
//                           88" . "88  
//                           (| -_- |)  
//                            O\ = /O  
//                        ____/`---'\____  
//                      .   ' \\| |// `.  
//                       / \\||| : |||// \  
//                     / _||||| -:- |||||- \  
//                       | | \\\ - /// | |  
//                     | \_| ''\---/'' | |  
//                      \ .-\__ `-` ___/-. /  
//                   ___`. .' /--.--\ `. . __  
//                ."" '< `.___\_<|>_/___.' >'"".  
//               | | : `- \`.;`\ _ /`;.`/ - ` : | |  
//                 \ \ `-. \_ __\ /__ _/ .-` / /  
//         ======`-.____`-.___\_____/___.-`____.-'======  
//                            `=---='  
//  
//         .............................................  
//                  佛祖保佑             永无BUG 
//          佛曰:  
//                  写字楼里写字间，写字间里程序员；  
//                  程序人员写程序，又拿程序换酒钱。  
//                  酒醒只在网上坐，酒醉还来网下眠；  
//                  酒醉酒醒日复日，网上网下年复年。  
//                  但愿老死电脑间，不愿鞠躬老板前；  
//                  奔驰宝马贵者趣，公交自行程序员。  
//                  别人笑我忒疯癫，我笑自己命太贱；  
//                  不见满街漂亮妹，哪个归得程序员？
*/
