package org.swu.vehiclecloud.dto;

import lombok.Data;

/**
 * API统一响应对象
 * @param <T> 响应数据的类型
 */
@Data
public class ApiResponse<T> {
    private int code;       // 响应状态码
    private String message; // 响应消息
    private T data;        // 响应数据

    /**
     * 成功响应
     * @param message 响应消息
     * @param data 响应数据
     * @return ApiResponse对象
     */
    public static <T> ApiResponse<T> success(String message, T data) {
        ApiResponse<T> response = new ApiResponse<>();
        response.setCode(200);
        response.setMessage(message);
        response.setData(data);
        return response;
    }

    /**
     * 错误响应
     * @param code 错误码
     * @param message 错误消息
     * @return ApiResponse对象
     */
    public static <T> ApiResponse<T> error(int code, String message) {
        ApiResponse<T> response = new ApiResponse<>();
        response.setCode(code);
        response.setMessage(message);
        return response;
    }
}