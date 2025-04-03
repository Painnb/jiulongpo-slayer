package org.swu.vehiclecloud.service;

import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

/**
 * 数据服务接口，定义数据流相关操作
 */
public interface DataService {
    /**
     * 创建并返回一个SSE数据流
     * @return SseEmitter 返回配置好的SSE发射器实例
     * 该发射器会每秒发送一次数据，直到连接关闭或超时
     * 可通过setPushContent方法动态更新推送内容
     */
    SseEmitter streamData();
    
    /**
     * 设置推送内容
     * @param content 要推送的内容
     * 设置后，所有已连接的SSE客户端将收到更新后的内容
     * 内容会每秒推送一次，直到再次更新
     */
    void setPushContent(String content);
}