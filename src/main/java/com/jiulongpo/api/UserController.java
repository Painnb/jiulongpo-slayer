package com.jiulongpo.api;

import com.jiulongpo.dto.ApiResponse;
import com.jiulongpo.dto.RegisterRequest;
import com.jiulongpo.exception.BusinessException;
import com.jiulongpo.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 用户控制器
 * 处理与用户相关的HTTP请求
 */
@RestController  // Spring注解，标记这是一个REST控制器
@RequestMapping("/user")  // 指定该控制器的基础URL路径
public class UserController {

    @Autowired
    private UserService userService;  // 注入用户服务

    /**
     * 处理用户注册请求
     * 接收POST请求，路径为 /api/user/register
     * 请求体需要包含username和password字段
     *
     * @param request 注册请求对象，包含用户名和密码
     * @return 包含注册结果的响应实体
     * @throws BusinessException 当用户名已存在或注册失败时抛出
     */
    @PostMapping("/register")  // 处理POST请求，路径为 /register
    public ResponseEntity<ApiResponse<Void>> register(@Validated @RequestBody RegisterRequest request) {
        // 调用服务层处理注册逻辑
        userService.register(request);
        // 返回成功响应
        return ResponseEntity.ok(ApiResponse.success("注册成功", null));
    }
}