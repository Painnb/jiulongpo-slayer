package org.swu.vehiclecloud.controller;

import org.swu.vehiclecloud.controller.template.ApiResult;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import org.swu.vehiclecloud.service.impl.UserServiceImpl;

import java.util.Map;

@RestController
@RequestMapping("/api/user")
@CrossOrigin(origins = "*")
public class UserManage {
    @Autowired
    private final UserServiceImpl userService;

    //    @Autowired
    public UserManage(UserServiceImpl userService) {
        this.userService = userService;
    }

    // 登录接口，前端传递username, password, token
    @GetMapping("/login")
    public ApiResult<Map<String, Object>> login(@RequestBody Map<String, Object> requestBody) {
        return userService.login(requestBody);
    }
}
