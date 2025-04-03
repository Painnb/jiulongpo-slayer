package org.swu.vehiclecloud.util;

import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * SSE工具类，用于创建和管理服务器发送事件(SSE)的发射器
 * 使用ScheduledExecutorService定期发送数据到客户端
 */
public class SSEUtil {
    private static final ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(1);

    /**
     * 创建一个新的SSE发射器
     * @return SseEmitter 返回配置好的SSE发射器实例
     * 该发射器会每秒发送一次当前设置的推送内容，直到连接关闭或超时
     * 发射器超时时间设置为Long.MAX_VALUE，表示不会自动超时
     * 使用单线程调度器确保推送顺序一致性
     * 可通过setPushContent方法动态更新推送内容
     */
    private static SseEmitter emitter;
    private static String pushContent;
    
    public static SseEmitter createEmitter() {
        emitter = new SseEmitter(Long.MAX_VALUE);
        
        // 设置定时任务，每秒发送一次数据
        scheduler.scheduleAtFixedRate(() -> {
            try {
                if (pushContent != null) {
                    emitter.send(pushContent);
                }
            } catch (IOException e) {
                // 发生IO异常时，完成发射器并关闭调度器
                emitter.completeWithError(e);
                scheduler.shutdown();
            }
        }, 0, 1, TimeUnit.SECONDS);
        
        // 设置完成和超时回调，关闭调度器
        emitter.onCompletion(() -> scheduler.shutdown());
        emitter.onTimeout(() -> scheduler.shutdown());
        
        return emitter;
    }
    
    /**
     * 设置推送内容
     * @param content 要推送的内容
     * 设置后，所有已连接的SSE发射器将开始推送新内容
     * 内容会每秒推送一次，直到再次更新
     */
    public static void setPushContent(String content) {
        pushContent = content;
    }
}