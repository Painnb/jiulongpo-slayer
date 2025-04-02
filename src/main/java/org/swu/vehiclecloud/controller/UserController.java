package org.swu.vehiclecloud.controller;

import org.springframework.security.access.prepost.PreAuthorize;
import org.swu.vehiclecloud.controller.template.ApiResult;
import org.swu.vehiclecloud.entity.User;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.swu.vehiclecloud.service.impl.UserServiceImpl;

import java.util.Map;

/**
 * 用户控制器
 * 处理与用户相关的HTTP请求
 */
@RestController  // Spring注解，标记这是一个REST控制器
@RequestMapping("/api/usermanage")  // 指定该控制器的基础URL路径
@CrossOrigin(origins = "*")
public class UserController {

    @Autowired
    private final UserServiceImpl userServiceImpl;  // 注入用户服务

    public UserController(UserServiceImpl userServiceImpl) {
        this.userServiceImpl = userServiceImpl;
    }

    /**
     * 处理用户注册请求
     * 接收POST请求，路径为 /api/usermanage/public/register
     * 请求体需要包含username和password字段
     *
     * @param user 用户DTO对象，包含用户名和密码
     * @return 包含注册结果的响应实体
     */
    @PostMapping("/public/register")  // 处理POST请求，路径为 /register
    public ResponseEntity<User> register(@RequestBody User user) {
        User registeredUser = userServiceImpl.register(user);
        return ResponseEntity.ok(registeredUser);
    }

    /**
     * 处理用户登录请求
     * 接收POST请求，路径为 /api/usermanage/public/login
     * 请求体需要包含username和password
     *
     * @param requestBody 请求体
     * @return 包含登录结果的响应实体
     */
    @PostMapping("/public/login")
    // @PreAuthorize("hasAnyRole('USER', 'SYS_ADMIN', 'BIZ_ADMIN', 'GUEST')")
    public ApiResult<Map<String, Object>> login(@RequestBody Map<String, Object> requestBody) {
        return userServiceImpl.login(requestBody);
    }

    /**
     * 获取用户信息
     * 接收GET请求，路径为 /api/usermanage/public/{id}
     * 请求参数需要包含用户ID
     *
     * @param id 用户ID
     * @return 包含用户信息的响应实体
     */
    @GetMapping("/public/{id}")
    public ResponseEntity<User> getUser(@PathVariable Integer id) {
        User user = userServiceImpl.getUserById(id);
        return ResponseEntity.ok(user);
    }

    /**
     * 更新用户信息
     * 接收PUT请求，路径为 /api/usermanage/public/{id}
     * 请求体需要包含用户DTO对象，包含用户ID
     *
     * @param id 用户ID
     * @param userDTO 用户DTO对象，包含用户信息
     * @return 包含更新后的用户信息的响应实体
     */
    @PutMapping("/public/{id}")
    public ResponseEntity<User> updateUser(@PathVariable Integer id, @RequestBody User user) {
        user.setId(null);
        User updatedUser = userServiceImpl.updateUser(user);
        return ResponseEntity.ok(updatedUser);
    }

    /**
     * 删除用户
     * 接收DELETE请求，路径为 /api/usermanage/public/{id}
     * 请求参数需要包含用户ID
     *
     * @param id 用户ID
     * @return 空响应实体
     */
    @DeleteMapping("/public/{id}")
    public ResponseEntity<Void> deleteUser(@PathVariable Integer id) {
        userServiceImpl.deleteUser(id);
        return ResponseEntity.ok().build();
    }
}