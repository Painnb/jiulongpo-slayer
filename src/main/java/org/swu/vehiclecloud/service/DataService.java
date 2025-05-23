package org.swu.vehiclecloud.service;

import io.swagger.annotations.Api;
import org.springframework.http.codec.ServerSentEvent;
import org.swu.vehiclecloud.controller.template.ApiResult;
import org.swu.vehiclecloud.dto.AnomalyStat;
import org.swu.vehiclecloud.dto.VehicleExceptionCount;
import reactor.core.publisher.Flux;


import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;


/**
 * 使用WebFlux处理服务器发送事件(SSE)数据流的服务接口。
 */
import org.springframework.transaction.annotation.Transactional;

@Transactional
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


    /**
     * 获取所有异常类型的统计信息
     * @return 包含异常类型和数量的列表
     */
    List<Map<String, Object>> getExceptionStatistics(LocalDateTime startTime, LocalDateTime endTime);

    /**
     * 获取指定时间范围内的异常数据
     * @param tableName 异常表名
     * @param startTime 开始时间
     * @param endTime 结束时间
     * @return 异常数据列表
     */
    List<Map<String, Object>> getExceptionDataWithTimeRange(
            String tableName,
            LocalDateTime startTime,
            LocalDateTime endTime);
    /**
     * 获取指定车辆和时间范围内的异常数据
     * @param tableName 异常表名
     * @param vehicleId 车辆ID
     * @param startTime 开始时间
     * @param endTime 结束时间
     * @return 异常数据列表
     */
    List<Map<String, Object>> getExceptionDataWithFilter(
            String tableName,
            String vehicleId,
            LocalDateTime startTime,
            LocalDateTime endTime);
  
    /**
     * 获取异常统计信息（按value从大到小排序）
     * @return 包含标题、数值、百分比和颜色值的数组
     */
    List<AnomalyStat> getExceptionPieData();

    /**
     * 获取各车辆异常数量统计
     * @return 车辆异常数量列表
     */
    List<VehicleExceptionCount> getVehicleExceptionCounts(LocalDateTime startTime, LocalDateTime endTime);

    /**
     * 获取机器学习异常数量统计
     * @return 机器学习检测的车辆异常数量统计列表
     */
    ApiResult<Map<String, Object>> getMlExceptionData(LocalDateTime startTime, LocalDateTime endTime);

    /**
     * 获取车辆在线时长
     * @return status message data
     */
    ApiResult<Map<String, Object>> getVehicleOnlineTimeRanking(LocalDateTime startTime, LocalDateTime endTime);

    /**
     * 获取2024.08.13-2024.08.15这三天车辆的在线数量和活跃数量
     * @return status message data
     */
    ApiResult<Map<String, Object>> getSevenDaysActivityData();
}

