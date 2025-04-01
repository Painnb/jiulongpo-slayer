package com.jiulongpo.dto;

import lombok.Data;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * 用户注册请求数据传输对象
 * 用于接收前端传来的注册请求数据
 */
@Data  // Lombok注解，自动生成getter、setter、toString等方法
public class RegisterRequest {

    /**
     * 用户名
     * 不能为空，长度在4-20个字符之间
     */
    @NotBlank(message = "用户名不能为空")
    @Size(min = 4, max = 20, message = "用户名长度必须在4-20个字符之间")
    private String username;

    /**
     * 密码
     * 不能为空，长度在6-20个字符之间
     */
    @NotBlank(message = "密码不能为空")
    @Size(min = 6, max = 20, message = "密码长度必须在6-20个字符之间")
    private String password;
}