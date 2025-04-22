package org.swu.vehiclecloud.service.impl;

import cn.hutool.core.util.StrUtil;
import lombok.Getter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.codec.ServerSentEvent;
import org.springframework.stereotype.Service;
import org.swu.vehiclecloud.controller.template.ApiResult;
import org.swu.vehiclecloud.dto.AnomalyStat;
import org.swu.vehiclecloud.dto.VehicleExceptionCount;

import org.swu.vehiclecloud.entity.MlExpcetion;
import org.swu.vehiclecloud.mapper.DataMapper;
import org.swu.vehiclecloud.service.DataService;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.core.publisher.Sinks;
import reactor.core.scheduler.Schedulers;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.*;
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
     * 如果该ID的数据流不存在，则创建一个新的数据流
     * 使用Sinks.Many作为内容发射器，支持多播和背压
     *
     * @param id 数据流标识符，区分不同的SSE通道
     * @return Flux<ServerSentEvent < String>> 包含服务器推送事件的响应式流
     * 会自动处理订阅和取消订阅事件
     */
    @Override
    public Flux<ServerSentEvent<String>> streamData(String id) {
        if (id == null || id.trim().isEmpty()) {
            log.error("无效的数据流ID: {}", id);
            return Flux.error(new IllegalArgumentException("数据流ID不能为空"));
        }

        try {
            log.debug("获取数据流 ID[{}], 当前活跃流数量: {}", id, activeStreams.size());
            return activeStreams.computeIfAbsent(id, this::createAndShareStreamForId)
                    .doOnSubscribe(subscription -> handleSubscribe(id))
                    .doOnCancel(() -> handleCancel(id))
                    .doOnError(e -> log.error("数据流处理异常 ID[{}]: {}", id, e.getMessage()));
        } catch (Exception e) {
            log.error("创建数据流失败 ID[{}]: {}", id, e.getMessage());
            return Flux.error(e);
        }
    }

    /**
     * 设置推送内容
     * 立即将内容推送给所有订阅该ID的客户端
     * 使用tryEmitNext进行高效推送，失败时自动重试
     *
     * @param id      目标数据流标识符，必须与streamData中的ID一致
     * @param content 要推送的内容（不可为null）
     * @throws IllegalArgumentException 如果content为null时抛出
     */
    @Override
    public void setPushContent(String id, String content) {
        if (content == null) {
            log.error("推送内容为空 ID[{}]", id);
            throw new IllegalArgumentException("推送内容不能为null");
        }

        if (id == null || id.trim().isEmpty()) {
            log.error("无效的数据流ID: {}", id);
            throw new IllegalArgumentException("数据流ID不能为空");
        }

        // 检查当前订阅者数量
        AtomicInteger counter = subscriberCounts.get(id);
        int subscriberCount = (counter != null) ? counter.get() : 0;
        if (subscriberCount <= 0) {
            log.info("当前无订阅者，推送内容被丢弃 ID[{}]: {}", id, content);
            return;
        }

        log.info("设置推送内容 ID[{}]: {}", id, content);

        try {
            Sinks.Many<String> sink = contentSinks.computeIfAbsent(id,
                    key -> {
                        log.debug("创建新的内容发射器 ID[{}]", key);
                        return Sinks.many().multicast().onBackpressureBuffer();
                    });

            // 确保立即推送新内容给所有订阅者
            Sinks.EmitResult result = sink.tryEmitNext(content);
            if (result.isFailure()) {
                log.warn("首次内容发送失败 ID[{}], 尝试重试", id);
                // 如果第一次发送失败，尝试使用emitComplete重试
                sink.emitNext(content, (signalType, emitResult) -> {
                    if (emitResult.isFailure()) {
                        log.error("内容发送失败 ID[{}]: {}", id, emitResult);
                        return false; // 不再重试
                    }
                    log.debug("重试发送成功 ID[{}]", id);
                    return true;
                });
            } else {
                log.debug("内容发送成功 ID[{}]", id);
            }
        } catch (Exception e) {
            log.error("设置推送内容异常 ID[{}]: {}", id, e.getMessage());
            throw e;
        }
    }

    /**
     * 创建并共享指定ID的数据流
     * 内部使用Sinks.Many作为内容发射源
     * 配置了1条消息的replay缓存和30秒的无订阅清理延迟
     *
     * @param id 数据流标识符
     * @return Flux<ServerSentEvent < String>> 新创建的SSE数据流
     * 会自动转换为SSE格式并添加事件ID
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
     *
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
     *
     * @param id 数据流标识符
     */
    private void handleSubscribe(String id) {
        try {
            AtomicInteger counter = subscriberCounts.computeIfAbsent(id,
                    k -> {
                        log.debug("初始化订阅计数器 ID[{}]", id);
                        return new AtomicInteger();
                    });
            int count = counter.incrementAndGet();
            log.info("新增订阅者 ID[{}], 当前总数: {}", id, count);

            // 冗余状态检查
            if (count <= 0) {
                log.warn("异常订阅计数 ID[{}]: {}", id, count);
                counter.set(1);
            }
        } catch (Exception e) {
            log.error("处理订阅事件异常 ID[{}]: {}", id, e.getMessage());
            throw e;
        }
    }

    /**
     * 处理取消订阅事件
     *
     * @param id 数据流标识符
     */
    private void handleCancel(String id) {
        try {
            AtomicInteger counter = subscriberCounts.get(id);
            if (counter == null) {
                log.warn("取消订阅时未找到计数器 ID[{}]", id);
                return;
            }

            int remaining = counter.decrementAndGet();
            log.info("取消订阅 ID[{}], 剩余订阅: {}", id, remaining);

            // 冗余状态检查
            if (remaining < 0) {
                log.warn("异常订阅计数 ID[{}]: {}", id, remaining);
                counter.set(0);
                remaining = 0;
            }

            if (remaining == 0) {
                log.debug("调度清理任务 ID[{}]", id);
                scheduleCleanup(id);
            }
        } catch (Exception e) {
            log.error("处理取消订阅异常 ID[{}]: {}", id, e.getMessage());
            throw e;
        }
    }

    /**
     * 调度资源清理任务
     *
     * @param id 需要清理的数据流标识符
     */
    private void scheduleCleanup(String id) {
        try {
            log.debug("开始调度清理任务 ID[{}]", id);
            Mono.delay(Duration.ofSeconds(30))
                    .doOnSubscribe(s -> log.debug("清理任务已调度 ID[{}]", id))
                    .subscribe(v -> {
                        try {
                            int count = subscriberCounts.getOrDefault(id, new AtomicInteger()).get();
                            if (count == 0) {
                                log.info("执行资源清理 ID: {}", id);
                                activeStreams.remove(id);
                                // contentSinks.remove(id);
                                subscriberCounts.remove(id);
                                log.debug("资源清理完成 ID[{}]", id);
                            } else {
                                log.debug("清理任务取消 ID[{}], 当前订阅数: {}", id, count);
                            }
                        } catch (Exception e) {
                            log.error("清理任务执行异常 ID[{}]: {}", id, e.getMessage());
                        }
                    }, e -> log.error("清理任务调度异常 ID[{}]: {}", id, e.getMessage()));
        } catch (Exception e) {
            log.error("调度清理任务异常 ID[{}]: {}", id, e.getMessage());
            throw e;
        }
    }
    

    @Autowired
    private DataMapper dataMapper;

    @Override
    public List<AnomalyStat> getExceptionPieData() {
        // 1. 从数据库获取各异常类型统计数
        List<AnomalyStat> stats = new ArrayList<>();
        stats.add(new AnomalyStat("方向盘异常", dataMapper.countSteeringAnomalies(), "#f25e43"));
        stats.add(new AnomalyStat("车速异常", dataMapper.countSpeedAnomalies(), "#00bcd4"));
        stats.add(new AnomalyStat("加速度异常", dataMapper.countAccelerationAnomalies(), "#64d572"));
        stats.add(new AnomalyStat("油门异常", dataMapper.countBrakeAnomalies(), "#ffeb3b"));
        stats.add(new AnomalyStat("发动机异常", dataMapper.countEngineAnomalies(), "#ff5722"));
        stats.add(new AnomalyStat("地理位置异常", dataMapper.countGeolocationAnomalies(), "#ff5722"));
        stats.add(new AnomalyStat("时间戳异常", dataMapper.countTimestampAnomalies(), "#ff5722"));

        // 2. 计算总异常数用于百分比计算
        int total = stats.stream().mapToInt(AnomalyStat::getValue).sum();

        // 3. 设置每个异常项的百分比（四舍五入）
        stats.forEach(stat -> {
            double percent = (total > 0) ? (stat.getValue() * 100.0 / total) : 0;
            stat.setPercent((int) Math.round(percent));
        });

        // 4. 按value从大到小排序
        stats.sort((a, b) -> Integer.compare(b.getValue(), a.getValue()));

        return stats;

    }


    @Override
    public List<VehicleExceptionCount> getVehicleExceptionCounts(LocalDateTime startTime, LocalDateTime endTime) {
        // 1. 从数据库获取各车辆异常数量统计（现在包含7个表）
        List<Map<String, Object>> rawData = dataMapper.countExceptionsByVehicle(startTime, endTime);

        // 2. 转换为VehicleExceptionCount对象列表
        List<VehicleExceptionCount> result = new ArrayList<>();
        for (Map<String, Object> item : rawData) {
            String vehicleId = (String) item.get("name");
            // 处理可能的null值
            long count = item.get("value") == null ? 0 : ((Number) item.get("value")).longValue();
            result.add(new VehicleExceptionCount("车辆" + vehicleId, (int) count));
        }

          

        // 4. 按异常数量从大到小排序
        result.sort((a, b) -> Integer.compare(b.getValue(), a.getValue()));

        return result;
    }



    @Override
    public List<Map<String, Object>> getExceptionStatistics (LocalDateTime startTime, LocalDateTime endTime) {
        List<Map<String, Object>> result = new ArrayList<>();

        // 获取所有异常表名
        List<String> exceptionTables = dataMapper.listExceptionTables();

        // 为每个异常表查询记录数
        for (String tableName : exceptionTables) {
            String exceptionName = tableName.replace("_exp", "") + "异常";
            List<Map<String, Object>> exceptionData = dataMapper.selectExceptionDataWithTimeRange(tableName, startTime, endTime);
            int count = exceptionData.size();
            result.add(Map.of("value", count, "name", exceptionName));
        }

        return result;
    }

    /**
     * 获取指定时间范围内的异常数据
     */
    public List<Map<String, Object>> getExceptionDataWithTimeRange (
            String tableName,
            LocalDateTime startTime,
            LocalDateTime endTime){
        return dataMapper.selectExceptionDataWithTimeRange(tableName, startTime, endTime);
    }

    /**
     * 获取指定车辆和时间范围内的异常数据
     */
    public List<Map<String, Object>> getExceptionDataWithFilter (
            String tableName,
            String vehicleId,
            LocalDateTime startTime,
            LocalDateTime endTime){
        return dataMapper.selectExceptionDataWithFilter(tableName, vehicleId, startTime, endTime);
    }

        /**
      * 获取机器学习异常数量统计
      * @return 机器学习检测的车辆异常数量统计列表
      */
      @Override
      public ApiResult<Map<String, Object>> getMlExceptionData(LocalDateTime startTime, LocalDateTime endTime) {
          try{
              List<MlExpcetion> mlExceptionData = dataMapper.selectMlExceptionData(startTime, endTime);
  
              if(!StrUtil.isEmptyIfStr(mlExceptionData)) {
                  // 存储 vehicleId 和 mse 的列表
                  List<String> vehicleIds = new ArrayList<>();
                  List<Double> mseValues = new ArrayList<>();
  
                  // 遍历 mlExceptionData，将每个 vehicleId 和 mse 添加到对应的列表
                  for (MlExpcetion exception : mlExceptionData) {
                      vehicleIds.add(exception.getVehicleId());
                      mseValues.add(exception.getMse());
                  }
  
                  // 将数据封装成需要的结构
                  Map<String, Object> resultData = new HashMap<>();
                  resultData.put("data", Arrays.asList(vehicleIds, mseValues));
  
                  // 返回包含 data 的 ApiResult
                  return ApiResult.of(200, "OK", resultData);
              }else{
                  return ApiResult.of(400, "Bad Request: No mlException data found", null);
              }
          }catch(NullPointerException e){
              throw new NullPointerException("Bad request. Missing required fields.");
          }catch (Exception e) {
              // 捕获其他异常并返回 500 错误
              return ApiResult.of(500, "Internal server error: " + e.getMessage(), null);
          }
      }

    @Override
    public ApiResult<Map<String, Object>> getVehicleOnlineTimeRanking(LocalDateTime startTime, LocalDateTime endTime) {
        try{
            List<Map<String, Object>> vehicleOnlineTimeData = dataMapper.selectVehicleOnlineTimeData(startTime, endTime);
            
            // 创建包含所有车辆排名的列表
            List<Map<String, Object>> rankingList = new ArrayList<>();
            
            for (Map<String, Object> data : vehicleOnlineTimeData) {
                String vehicleId = (String) data.get("vehicleId");
                // 安全处理Long到Integer的转换
                Number onlineTime = (Number) data.get("no_data_alert_count");
                int onlineTimeValue = onlineTime != null ? onlineTime.intValue() : 0;
                
                rankingList.add(Map.of(
                    "vehicleId", vehicleId,
                    "onlineTime", onlineTimeValue
                ));
            }
        
            // 按在线时间从高到低排序
            rankingList.sort((a, b) -> Integer.compare(
                (Integer)b.get("onlineTime"), 
                (Integer)a.get("onlineTime")
            ));
        
            // 构建最终返回结果
            Map<String, Object> resultData = new HashMap<>();
            resultData.put("ranking", rankingList);
            
            return ApiResult.of(200, "OK", resultData);
        }catch(NullPointerException e){
            throw new NullPointerException("Bad request. Missing required fields.");
        }catch (Exception e) {
            // 捕获其他异常并返回 500 错误
            return ApiResult.of(500, "Internal server error: " + e.getMessage(), null);
        }
    }

    @Override
    public ApiResult<Map<String, Object>> getSevenDaysActivityData() {
        try{
            // 设置2024年8月13日的起始时间和结束时间
            LocalDateTime startTimeAug13 = LocalDateTime.of(2024, 8, 13, 0, 0, 0, 0); // 2024-08-13 00:00:00
            LocalDateTime endTimeAug13 = LocalDateTime.of(2024, 8, 13, 23, 59, 59, 999999); // 2024-08-13 23:59:59

            // 设置2024年8月14日的起始时间和结束时间
            LocalDateTime startTimeAug14 = LocalDateTime.of(2024, 8, 14, 0, 0, 0, 0); // 2024-08-14 00:00:00
            LocalDateTime endTimeAug14 = LocalDateTime.of(2024, 8, 14, 23, 59, 59, 999999); // 2024-08-14 23:59:59

            // 设置2024年8月15日的起始时间和结束时间
            LocalDateTime startTimeAug15 = LocalDateTime.of(2024, 8, 15, 0, 0, 0, 0); // 2024-08-15 00:00:00
            LocalDateTime endTimeAug15 = LocalDateTime.of(2024, 8, 15, 23, 59, 59, 999999); // 2024-08-15 23:59:59

            // 查询每个日期内的在线车数量
            int countOnlineAug13 = dataMapper.selectCountOnlineVehicle(startTimeAug13, endTimeAug13);
            int countOnlineAug14 = dataMapper.selectCountOnlineVehicle(startTimeAug14, endTimeAug14);
            int countOnlineAug15 = dataMapper.selectCountOnlineVehicle(startTimeAug15, endTimeAug15);

            // 查询每个日期内的活跃车数量
            int countActivityAug13 = dataMapper.selectCountActivityVehicle(startTimeAug13, endTimeAug13);
            int countActivityAug14 = dataMapper.selectCountActivityVehicle(startTimeAug14, endTimeAug14);
            int countActivityAug15 = dataMapper.selectCountActivityVehicle(startTimeAug15, endTimeAug15);

            Map<String, Object> resultData = new HashMap<>();
            resultData.put("2024-08-13", new DateVehicleCount(countOnlineAug13, countActivityAug13 * 2));
            resultData.put("2024-08-14", new DateVehicleCount(countOnlineAug14, countActivityAug14 * 2));
            resultData.put("2024-08-15", new DateVehicleCount(countOnlineAug15, countActivityAug15 * 2));
            return ApiResult.of(200, "OK", resultData);

        }catch(NullPointerException e){
            throw new NullPointerException("Bad request. Missing required fields.");
        }catch (Exception e) {
            // 捕获其他异常并返回 500 错误
            return ApiResult.of(500, "Internal server error: " + e.getMessage(), null);
        }
    }

    // 内部类用于表示每个日期的在线和活跃车辆数量
    @Getter
    public static class DateVehicleCount {
        private int onlineCount;
        private int activityCount;

        public DateVehicleCount(int onlineCount, int activityCount) {
            this.onlineCount = onlineCount;
            this.activityCount = activityCount;
        }

    }
}

