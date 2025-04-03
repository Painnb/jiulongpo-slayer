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
     * 创建并返回一个SSE数据流
     * @return SseEmitter 返回配置好的SSE发射器实例
     * 该方法使用SSEUtil工具类创建发射器实例
     */
    @Override
    public SseEmitter streamData(String id) {
        return SSEUtil.createEmitter(id);
    }
    
    @Override
    public void setPushContent(String id, String content) {
        SSEUtil.setPushContent(id, content);
    }
}