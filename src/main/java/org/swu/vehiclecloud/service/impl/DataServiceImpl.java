package org.swu.vehiclecloud.service.impl;

import org.springframework.stereotype.Service;
import org.swu.vehiclecloud.service.DataService;
import org.swu.vehiclecloud.util.SSEUtil;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

/**
 * DataService接口的实现类，提供数据流相关服务的具体实现
 */
@Service
public class DataServiceImpl implements DataService {
    /**
     * 创建并返回指定客户端的SSE数据流
     * @param id 客户端唯一标识符，用于关联后续的推送内容
     * @return SseEmitter 配置好的SSE长连接发射器
     * @implNote 实际委托SSEUtil创建发射器，默认超时时间为Long.MAX_VALUE
     * @see SSEUtil#createEmitter(String)
     */
    @Override
    public SseEmitter streamData(String id) {
        return SSEUtil.createEmitter(id);
    }
    
    /**
     * 设置指定客户端的推送内容
     * @param id 通过streamData方法获取的客户端标识符
     * @param content 需要推送的字符串内容
     * @apiNote 内容设置后会在下次定时任务执行时自动推送，更新内容会覆盖之前的值
     */
    @Override
    public void setPushContent(String id, String content) {
        SSEUtil.setPushContent(id, content);
    }
}