package org.swu.vehiclecloud.exception;

import lombok.Getter;

/**
 * 业务逻辑异常类
 * 用于处理业务逻辑相关的异常情况
 */
public class BusinessException extends RuntimeException {

    /**
     * -- GETTER --
     *  获取错误码
     *
     */
    @Getter
    private final int code;  // 错误码
    private final String message;  // 错误信息

    /**
     * 构造函数
     * @param message 错误信息
     */
    public BusinessException(String message) {
        super(message);
        this.code = 500;
        this.message = message;
    }

    /**
     * 构造函数
     * @param code 错误码
     * @param message 错误信息
     */
    public BusinessException(int code, String message) {
        super(message);
        this.code = code;
        this.message = message;
    }

    /**
     * 获取错误信息
     * @return 错误信息
     */
    @Override
    public String getMessage() {
        return message;
    }
}