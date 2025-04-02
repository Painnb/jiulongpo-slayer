package org.swu.vehiclecloud.controller;

import org.swu.vehiclecloud.dto.UserDTO;
import org.swu.vehiclecloud.entity.User;
import org.swu.vehiclecloud.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/**
 * 用户控制器
 * 处理与用户相关的HTTP请求
 */
@RestController  // Spring注解，标记这是一个REST控制器
@RequestMapping("/api/users")  // 指定该控制器的基础URL路径
@CrossOrigin(origins = "*")
public class UserController {

    @Autowired
    private UserService userService;  // 注入用户服务

    /**
     * 处理用户注册请求
     * 接收POST请求，路径为 /api/users/register
     * 请求体需要包含username和password字段
     *
     * @param userDTO 用户DTO对象，包含用户名和密码
     * @return 包含注册结果的响应实体
     */
    @PostMapping("/register")  // 处理POST请求，路径为 /register
    public ResponseEntity<User> register(@RequestBody UserDTO userDTO) {
        User user = userService.register(userDTO);
        return ResponseEntity.ok(user);
    }

    /**
     * 处理用户登录请求
     * 接收POST请求，路径为 /api/users/login
     * 请求参数需要包含username和password
     *
     * @param username 用户名
     * @param password 密码
     * @return 包含登录结果的响应实体
     */
    @PostMapping("/login")
    public ResponseEntity<User> login(@RequestParam String username, @RequestParam String password) {
        User user = userService.login(username, password);
        return ResponseEntity.ok(user);
    }

    /**
     * 获取用户信息
     * 接收GET请求，路径为 /api/users/{id}
     * 请求参数需要包含用户ID
     *
     * @param id 用户ID
     * @return 包含用户信息的响应实体
     */
    @GetMapping("/{id}")
    public ResponseEntity<User> getUser(@PathVariable Long id) {
        User user = userService.getUserById(id);
        return ResponseEntity.ok(user);
    }

    /**
     * 更新用户信息
     * 接收PUT请求，路径为 /api/users/{id}
     * 请求体需要包含用户DTO对象，包含用户ID
     *
     * @param id 用户ID
     * @param userDTO 用户DTO对象，包含用户信息
     * @return 包含更新后的用户信息的响应实体
     */
    @PutMapping("/{id}")
    public ResponseEntity<User> updateUser(@PathVariable Long id, @RequestBody UserDTO userDTO) {
        userDTO.setId(id);
        User user = userService.updateUser(userDTO);
        return ResponseEntity.ok(user);
    }

    /**
     * 删除用户
     * 接收DELETE请求，路径为 /api/users/{id}
     * 请求参数需要包含用户ID
     *
     * @param id 用户ID
     * @return 空响应实体
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteUser(@PathVariable Long id) {
        userService.deleteUser(id);
        return ResponseEntity.ok().build();
    }
}