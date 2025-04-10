package org.swu.vehiclecloud.test;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.SpyBean;
import org.springframework.context.ApplicationEventPublisher;
import org.swu.vehiclecloud.event.MqttMessageEvent;
import org.swu.vehiclecloud.listener.MqttMessageListener;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.timeout;
import static org.mockito.Mockito.verify;

@SpringBootTest
@ExtendWith(MockitoExtension.class)
class MqttEventIntegrationTest {

    @Autowired
    private ApplicationEventPublisher eventPublisher;

    @SpyBean // 监听器会被Spring自动注册
    private MqttMessageListener mqttMessageListener;

    /*
    @Test
    void shouldTriggerListenerWhenEventPublished() {
        // 准备测试事件
        String testTopic = "alert/fire";
        //String testMessage = "Fire detected!";
        //MqttMessageEvent event = new MqttMessageEvent(this, testTopic, testMessage);

        // 发布事件
        eventPublisher.publishEvent(event);

        // 验证监听器被调用
        verify(mqttMessageListener, timeout(1000))
                .handleMqttMessage(argThat(e ->
                        e.getTopic().equals(testTopic) &&
                                e.getMessage().equals(testMessage)
                ));

        // 验证特定主题处理逻辑
        //verify(mqttMessageListener).handleAlertMessage(any());
    }
     */

}