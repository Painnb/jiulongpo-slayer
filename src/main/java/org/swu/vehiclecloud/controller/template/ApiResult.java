package org.swu.vehiclecloud.controller.template;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Getter;
import lombok.Setter;

@Setter
@Getter
@ApiModel(value = "统一响应对象", description = "返回给前端的通用响应格式")
public class ApiResult<T> {

    @ApiModelProperty(value = "响应状态码", example = "200", required = true)
    private int status;

    @ApiModelProperty(value = "响应消息", example = "操作成功", required = true)
    private String message;

    @ApiModelProperty(value = "响应数据")
    private T data;

    // 私有构造函数，防止直接实例化
    private ApiResult(int status, String message, T data) {
        this.status = status;
        this.message = message;
        this.data = data;
    }

    /**
     * 通用的静态构造方法,适用于不需要给前端返回数据的时候调用
     *
     * @param status   状态码
     * @param message 消息
     * @return ApiResult对象
     */
    public static <T> ApiResult<T> of(int status, String message) {
        return new ApiResult<>(status, message,null);
    }

    /**
     * 通用的静态构造方法,适用于需要给前端返回数据的时候调用
     *
     * @param status   状态码
     * @param message 消息
     * @param data    数据
     * @return ApiResult对象
     */
    public static <T> ApiResult<T> of(int status, String message, T data) {
        return new ApiResult<>(status, message, data);
    }

    // Getter 和 Setter 方法

}

