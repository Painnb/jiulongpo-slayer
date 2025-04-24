package org.swu.vehiclecloud.listener;

import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.event.EventListener;
import org.springframework.dao.DataAccessException;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;
import org.swu.vehiclecloud.entity.MqttData;
import org.swu.vehiclecloud.event.MqttMessageEvent;
import org.swu.vehiclecloud.mapper.MqttMapper;

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

@Component
public class UploadMqttData {
    private final MqttMapper mqttMapper;
    // Redis缓存模板
    // @Autowired
    // private RedisTemplate<String, Object> redisTemplate;

    // Constructor injection
    @Autowired
    public UploadMqttData(MqttMapper mqttMapper) {
        this.mqttMapper = mqttMapper;
    }

    // 该参数控制是否执行数据库插入
    private boolean isRunning = true;

    // 创建一个定时任务调度器
    private final ScheduledExecutorService scheduler = Executors.newSingleThreadScheduledExecutor();

    // 日志记录的类
    private static final Logger logger = LoggerFactory.getLogger(UploadMqttData.class);

    @PostConstruct
    public void startTimer() {
        // 设置项目启动一分钟后停止执行 uploadMqttData 方法
        scheduler.schedule(() -> isRunning = false, 1, TimeUnit.MINUTES);
        
        // Redis定时批量插入任务(暂不启用)
        /*
        scheduler.scheduleAtFixedRate(() -> {
            try {
                // 从Redis获取所有缓存数据并批量插入数据库
                // Set<String> keys = redisTemplate.keys("mqtt:data:*");
                // if (keys != null && !keys.isEmpty()) {
                //     List<MqttData> dataList = new ArrayList<>();
                //     for (String key : keys) {
                //         String message = (String) redisTemplate.opsForValue().get(key);
                //         dataList.add(new MqttData(message));
                //         redisTemplate.delete(key);
                //     }
                //     mqttMapper.batchInsert(dataList);
                // }
            } catch (Exception e) {
                logger.error("Redis批量插入失败: " + e.getMessage());
            }
        }, 5, 5, TimeUnit.MINUTES);
        */
    }

    @EventListener
    public void uploadMqttData(MqttMessageEvent event) {
        if (!isRunning) {
            return;  // 如果停止执行，方法将不再处理事件
        }
        try{
            // 将mqtt数据插入到数据库
            //若要启用redis缓存，注释掉下面这行
            mqttMapper.insert(new MqttData(event.getMessageAsString()));
            
            // Redis缓存实现(暂不启用)
            /*
            String key = "mqtt:data:" + System.currentTimeMillis();
            redisTemplate.opsForValue().set(key, event.getMessageAsString());
            redisTemplate.expire(key, 1, TimeUnit.HOURS);
            */
        }catch(DataAccessException e){
            // 记录数据库操作的错误日志
            logger.error(e.getMessage());
        }
    }
}
