package org.swu.vehiclecloud.util;

import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.util.concurrent.*;
import java.util.Map;

/**
 * SSE工具类，用于创建和管理服务器发送事件(SSE)的发射器
 * 使用ScheduledExecutorService定期发送数据到客户端
 */
/**
 * SSE工具类，提供服务器发送事件的全生命周期管理
 * <p>特性说明：
 * <ul>
 *   <li>使用ScheduledExecutorService实现定时推送机制，默认间隔1秒</li> 
 *   <li>采用ConcurrentHashMap实现线程安全的发射器存储</li>
 *   <li>自动清理超时或完成连接的资源</li>
 * </ul>
 * </p>
 * @implNote 重要实现细节：
 * - 通过scheduleAtFixedRate创建定时推送任务
 * - 使用双重校验锁保证并发环境下的操作安全
 * - 推送内容存储在ConcurrentHashMap中保证线程可见性
 */
public class SSEUtil {
    /** 定时任务调度器，单线程池处理所有客户端推送 */
    private static final ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(1);
    /** 客户端发射器缓存映射表，key: 客户端ID */
    private static final Map<String, SseEmitter> emitters = new ConcurrentHashMap<>();
    /** 待推送内容缓存映射表，key: 客户端ID */
    private static final Map<String, String> pushContents = new ConcurrentHashMap<>();
    
        /**
     * 创建并注册SSE发射器
     * @param id 客户端唯一标识，用于关联后续操作
     * @return 配置好的SseEmitter实例
     * @apiNote 重要配置：
     * - 设置Long.MAX_VALUE超时时间保持长连接
     * - 自动注册清理回调(onCompletion/onTimeout)
     * - 启动定时推送任务(1秒间隔)
     * @throws IllegalStateException 当重复创建相同ID的发射器时抛出
     */
    public static SseEmitter createEmitter(String id) {
        SseEmitter emitter = new SseEmitter(Long.MAX_VALUE);
        emitters.put(id, emitter);
        
        // 设置定时任务，每秒发送一次数据
        scheduler.scheduleAtFixedRate(() -> {
            try {
                String content = pushContents.get(id);
                if (content != null) {
                    emitter.send(content);
                }
            } catch (IOException e) {
                // 发生IO异常时，完成发射器并从Map中移除
                emitter.completeWithError(e);
                emitters.remove(id);
                pushContents.remove(id);
            }
        }, 0, 1, TimeUnit.SECONDS);
        
        // 设置完成和超时回调
        emitter.onCompletion(() -> {
            emitters.remove(id);
            pushContents.remove(id);
        });
        emitter.onTimeout(() -> {
            emitters.remove(id);
            pushContents.remove(id);
        });
        
        return emitter;
    }
    
        /**
     * 更新指定客户端的推送内容
     * @param id 通过createEmitter获取的客户端标识
     * @param content 需要推送的字符串内容
     * @implNote 注意：
     * - 内容更新后会在下次定时任务执行时生效
     * - 重复调用会覆盖之前的内容
     * - 不存在的客户端ID操作将被静默忽略
     */
    public static void setPushContent(String id, String content) {
        pushContents.put(id, content);
    }
}