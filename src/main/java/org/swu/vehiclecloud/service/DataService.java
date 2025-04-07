package org.swu.vehiclecloud.service;

import org.springframework.http.codec.ServerSentEvent;
import reactor.core.publisher.Flux;

/**
 * 使用WebFlux处理服务器发送事件(SSE)数据流的服务接口。
 */
public interface DataService {

    /**
     * 为给定ID创建或获取共享的SSE数据流。
     * 使用相同ID连接的多个客户端将接收到相同的事件流。
     * 该流每秒根据当前为该ID设置的内容发送数据。
     *
     * @param id SSE流的唯一标识符。
     * @return 发射包含数据的ServerSentEvent对象的Flux流。
     */
    Flux<ServerSentEvent<String>> streamData(String id);

    /**
     * 设置或更新特定SSE流ID要推送的内容。
     * 此内容将用于后续对该ID所有已连接客户端的推送。
     * 如果未设置内容，将使用默认值("test")。
     * 即使当前没有客户端连接该ID，也可以调用此方法。
     *
     * @param id      SSE流的唯一标识符。
     * @param content 要推送的新内容。
     */
    void setPushContent(String id, String content);
}