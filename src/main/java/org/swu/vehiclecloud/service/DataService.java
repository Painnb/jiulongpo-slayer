package org.swu.vehiclecloud.service;

import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;
import org.swu.vehiclecloud.service.impl.DataServiceImpl;

/**
 * SSE数据流服务接口
 * <p>定义服务器发送事件(Server-Sent Events)的核心操作，通过实现类提供具体实现</p>
 * @see DataServiceImpl 默认实现类
 */
public interface DataService {
    /**
     * 创建并返回一个SSE数据流
     * @return SseEmitter 返回配置好的SSE发射器实例
     * 该发射器会每秒发送一次数据，直到连接关闭或超时
     * 可通过setPushContent方法动态更新推送内容
     */
    SseEmitter streamData(String id);
    
    /**
     * 设置指定客户端的推送内容
     * @param id 通过streamData方法获取的客户端标识符
     * @param content 需要推送的字符串内容
     * @throws IllegalArgumentException 当id对应的发射器不存在时抛出
     * @apiNote 内容设置后会在下次定时任务执行时自动推送，更新内容会覆盖之前的值
     */
    void setPushContent(String id, String content);
}