package org.swu.vehiclecloud.util;

import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.util.concurrent.*;
import java.util.Map;
import java.util.HashMap;
import java.util.UUID;

/**
 * SSE工具类，用于创建和管理服务器发送事件(SSE)的发射器
 * 使用ScheduledExecutorService定期发送数据到客户端
 */
public class SSEUtil {
    private static final ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(1);
    private static final Map<String, SseEmitter> emitters = new ConcurrentHashMap<>();
    private static final Map<String, String> pushContents = new ConcurrentHashMap<>();
    
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
    
    public static void setPushContent(String id, String content) {
        pushContents.put(id, content);
    }
}