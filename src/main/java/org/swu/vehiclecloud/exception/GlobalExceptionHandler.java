package org.swu.vehiclecloud.exception;

import org.swu.vehiclecloud.controller.template.ApiResult;
import org.swu.vehiclecloud.dto.ApiResponse;
import org.springframework.http.HttpStatus;
import org.springframework.validation.BindException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 全局异常处理器
 * 统一处理系统中抛出的各种异常，并转换为统一的响应格式
 */
@RestControllerAdvice  // Spring注解，标记这是一个全局异常处理器
public class GlobalExceptionHandler {

  /**
   * 处理业务逻辑异常
   * @param ex 业务逻辑异常
   * @return 包含错误信息的响应对象
   */
  @ExceptionHandler(BusinessException.class)
  @ResponseStatus(HttpStatus.BAD_REQUEST)  // 设置响应状态码为400
  public ApiResponse<Void> handleBusinessException(BusinessException ex) {
    return ApiResponse.error(ex.getCode(), ex.getMessage());
  }

  /**
   * 处理参数验证异常
   * 当使用@Valid注解的对象验证失败时触发
   * @param ex 参数验证异常
   * @return 包含验证错误信息的响应对象
   */
  @ExceptionHandler(MethodArgumentNotValidException.class)
  @ResponseStatus(HttpStatus.BAD_REQUEST)
  public ApiResponse<Void> handleValidationExceptions(MethodArgumentNotValidException ex) {
    // 获取所有字段错误
    List<FieldError> fieldErrors = ex.getBindingResult().getFieldErrors();
    // 将错误信息拼接成字符串
    String message = fieldErrors.stream()
            .map(error -> error.getField() + ": " + error.getDefaultMessage())
            .collect(Collectors.joining(", "));
    return ApiResponse.error(400, message);
  }

  /**
   * 处理参数绑定异常
   * 当请求参数无法正确绑定到对象时触发
   * @param ex 参数绑定异常
   * @return 包含绑定错误信息的响应对象
   */
  @ExceptionHandler(BindException.class)
  @ResponseStatus(HttpStatus.BAD_REQUEST)
  public ApiResponse<Void> handleBindException(BindException ex) {
    List<FieldError> fieldErrors = ex.getBindingResult().getFieldErrors();
    String message = fieldErrors.stream()
            .map(error -> error.getField() + ": " + error.getDefaultMessage())
            .collect(Collectors.joining(", "));
    return ApiResponse.error(400, message);
  }

  /**
   * 处理所有未捕获的异常
   * 作为最后的异常处理器
   * @param ex 未捕获的异常
   * @return 包含错误信息的响应对象
   */
  @ExceptionHandler(Exception.class)
  @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)  // 设置响应状态码为500
  public ApiResponse<Void> handleAllUncaughtException(Exception ex) {
    return ApiResponse.error(500, "服务器内部错误：" + ex.getMessage());
  }

  /**
   * 处理所有未捕获的异常
   * 作为最后的异常处理器
   * @return 包含自定义的错误信息的响应对象
   */
  @ExceptionHandler(AccessDeniedException.class)
  public ApiResult<Map<String, Object>> handleAccessDeniedException() {
    return ApiResult.of(401, "Access Denied: Insufficient Permissions",
            null);
  }

  /**
   * 处理所有未捕获的异常
   * 作为最后的异常处理器
   * @return 包含自定义的错误信息的响应对象
   */
  @ExceptionHandler(JwtIsExpiredException.class)
  public ApiResult<Map<String, Object>> JwtIsExpiredException() {
    return ApiResult.of(401, "Access Denied: Jwt Token is expired",
            null);
  }

  /**
   * 处理所有未捕获的异常
   * 作为最后的异常处理器
   * @return 包含自定义的错误信息的响应对象
   */
  @ExceptionHandler(JwtParseFailedException.class)
  public ApiResult<Map<String, Object>> JwtParseFailedException() {
    return ApiResult.of(500, "Server error: Jwt parse failed",
            null);
  }
}