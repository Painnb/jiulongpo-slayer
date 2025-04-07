package org.swu.vehiclecloud.service.impl;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.codec.ServerSentEvent;
import org.springframework.stereotype.Service;
import org.swu.vehiclecloud.service.DataService;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.core.publisher.Sinks;
import reactor.core.scheduler.Schedulers;

import java.time.Duration;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

/**
 * SSE数据流服务实现类
 * <p>基于WebFlux实现服务器推送事件(SSE)功能，支持按ID区分的独立数据流</p>
 */
@Service
public class DataServiceImpl implements DataService {

    private static final Logger log = LoggerFactory.getLogger(DataServiceImpl.class);
    
    /**
     * 内容发射器缓存
     * <p>Key: 数据流ID，Value: 对应内容发射器</p>
     */
    private final Map<String, Sinks.Many<String>> contentSinks = new ConcurrentHashMap<>();
    
    /**
     * 活跃数据流缓存
     * <p>Key: 数据流ID，Value: 对应的SSE数据流</p>
     */
    private final Map<String, Flux<ServerSentEvent<String>>> activeStreams = new ConcurrentHashMap<>();
    
    /**
     * 全局事件ID生成器
     */
    private final AtomicLong eventIdCounter = new AtomicLong();
    
    /**
     * 订阅者计数器
     * <p>Key: 数据流ID，Value: 当前订阅数</p>
     */
    private final Map<String, AtomicInteger> subscriberCounts = new ConcurrentHashMap<>();

    /**
     * 获取指定ID的SSE数据流
     * @param id 数据流标识符
     * @return 包含服务器推送事件的Flux流
     */
    @Override
    public Flux<ServerSentEvent<String>> streamData(String id) {
        return activeStreams.computeIfAbsent(id, this::createAndShareStreamForId)
                .doOnSubscribe(subscription -> handleSubscribe(id))
                .doOnCancel(() -> handleCancel(id));
    }

    /**
     * 设置推送内容
     * @param id 目标数据流标识符
     * @param content 要推送的内容（不可为null）
     * @throws IllegalArgumentException 如果content为null时抛出
     */
    @Override
    public void setPushContent(String id, String content) {
        if (content == null) {
            throw new IllegalArgumentException("推送内容不能为null");
        }
        
        log.info("设置推送内容 ID[{}]: {}", id, content);
        Sinks.Many<String> sink = contentSinks.computeIfAbsent(id, 
            key -> Sinks.many().multicast().onBackpressureBuffer());
        
        // 确保立即推送新内容给所有订阅者
        Sinks.EmitResult result = sink.tryEmitNext(content);
        if (result.isFailure()) {
            // 如果第一次发送失败，尝试使用emitComplete重试
            sink.emitNext(content, (signalType, emitResult) -> {
                if (emitResult.isFailure()) {
                    log.error("内容发送失败 ID[{}]: {}", id, emitResult);
                    return false; // 不再重试
                }
                return true;
            });
        }
    }

    /**
     * 创建并共享指定ID的数据流
     * @param id 数据流标识符
     * @return 新创建的SSE数据流
     */
    private Flux<ServerSentEvent<String>> createAndShareStreamForId(String id) {
        log.info("创建新SSE数据流 ID: {}", id);
    
        Sinks.Many<String> contentSink = contentSinks.computeIfAbsent(id, 
            key -> Sinks.many().multicast().onBackpressureBuffer());
    
        // 直接监听 Sinks 的数据流，并转换为 SSE 事件
        return contentSink.asFlux()
                .map(this::buildSSE)
                .publishOn(Schedulers.boundedElastic())
                .replay(1) // 缓存最近的一条消息，供新订阅者使用
                .refCount(1, Duration.ofSeconds(30)); // 在无订阅者时延迟清理资源
    }

    /**
     * 构建SSE事件对象
     * @param content 事件内容
     * @return 完整的ServerSentEvent对象
     */
    private ServerSentEvent<String> buildSSE(String content) {
        return ServerSentEvent.<String>builder()
                .id(String.valueOf(eventIdCounter.incrementAndGet()))
                .event("message")
                .data(content)
                .build();
    }

    /**
     * 处理订阅事件
     * @param id 数据流标识符
     */
    private void handleSubscribe(String id) {
        subscriberCounts.computeIfAbsent(id, k -> new AtomicInteger()).incrementAndGet();
        log.info("新增订阅者 ID[{}], 当前总数: {}", id, subscriberCounts.get(id).get());
    }

    /**
     * 处理取消订阅事件
     * @param id 数据流标识符
     */
    private void handleCancel(String id) {
        AtomicInteger counter = subscriberCounts.get(id);
        if (counter == null) return;

        int remaining = counter.decrementAndGet();
        log.info("取消订阅 ID[{}], 剩余订阅: {}", id, remaining);

        if (remaining == 0) {
            scheduleCleanup(id);
        }
    }

    /**
     * 调度资源清理任务
     * @param id 需要清理的数据流标识符
     */
    private void scheduleCleanup(String id) {
        Mono.delay(Duration.ofSeconds(30))
                .subscribe(v -> {
                    if (subscriberCounts.getOrDefault(id, new AtomicInteger()).get() == 0) {
                        log.info("执行资源清理 ID: {}", id);
                        activeStreams.remove(id);
                        contentSinks.remove(id);
                        subscriberCounts.remove(id);
                    }
                });
    }
}
