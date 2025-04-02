package org.swu.vehiclecloud.service.impl;

import cn.hutool.core.util.StrUtil;
import org.swu.vehiclecloud.controller.template.ApiResult;
import org.swu.vehiclecloud.dto.UserDTO;
import org.swu.vehiclecloud.entity.User;
import org.swu.vehiclecloud.mapper.UserMapper;
import org.swu.vehiclecloud.service.UserService;
import org.swu.vehiclecloud.util.JwtTokenProvider;
import org.swu.vehiclecloud.util.PasswordValidator;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;
import java.util.Random;

/**
 * 用户服务实现类
 */
@Service
public class UserServiceImpl implements UserService {
    @Autowired
    private UserMapper userMapper;

    @Autowired
    private final JwtTokenProvider tokenProvider;

    public UserServiceImpl(UserMapper userMapper, JwtTokenProvider tokenProvider) {
        this.userMapper = userMapper;
        this.tokenProvider = tokenProvider;
    }

    public ApiResult<Map<String, Object>> login(Map<String, Object> requestBody) {
        // 获取参数
        Map<String, Object> response = new HashMap<>();

        try {
            // 参数缺失
            if(!requestBody.containsKey("username") ||
                    !requestBody.containsKey("password")){
                return ApiResult.of(400, "400 Bad Request:参数缺失");
            }

            // 解析参数
            String username = requestBody.get("username").toString();
            String password = requestBody.get("password").toString();

            // 用户名或密码不能空
            if(StrUtil.isEmptyIfStr(username) || StrUtil.isEmptyIfStr(password)){
                return ApiResult.of(400, "400 Bad Request:参数为空");
            }

            // 调用密码加密
            String encryptedPassword = encryptPassword(password);

            // 与数据库匹配
            User user = userMapper.findByUsernameAndPassword(username, encryptedPassword);

            if(!StrUtil.isEmptyIfStr(user)){
                String token = tokenProvider.generateToken(user.getId());
                // 登陆成功，生成JWT token, 并返回成功结果给前端
                response.put("token",token);
                response.put("id",user.getId());
                response.put("username",user.getUsername());
                response.put("role",user.getRole());
                response.put("email",user.getEmail());
                response.put("created_time", user.getCreated_time());
                return ApiResult.of(200,"200 OK:登陆成功", response);
            }else{
                // 登录失败，不生成JWT token， 并返回错误结果给前端
                response.put("token",null);
                response.put("id",null);
                response.put("username",null);
                response.put("role",null);
                response.put("email",null);
                response.put("created_time",null);
                return ApiResult.of(401,"401 Unauthorized:用户名或密码错误", response);
            }
        }catch (Exception e){
            return ApiResult.of(500, e.getMessage());
        }
    }

    // 使用SHA256加密密码
    private String encryptPassword(String password){
        try {
            // 创建一个 MessageDigest 实例，指定使用 SHA-256 算法
            MessageDigest digest = MessageDigest.getInstance("SHA-256");

            // 获取密码的字节数组并进行加密
            byte[] hashedBytes = digest.digest(password.getBytes());

            // 将加密后的字节数组转换为十六进制字符串
            StringBuilder hexString = new StringBuilder();
            for (byte b : hashedBytes) {
                // 将每个字节转换为 2 位的十六进制表示
                hexString.append(String.format("%02x", b));
            }

            // 返回加密后的密码（十六进制字符串）
            return hexString.toString();
        } catch (NoSuchAlgorithmException e) {
            // 异常处理：若算法不可用
            throw new RuntimeException("SHA-256 algorithm not available", e);
        }
    }

    // 随机生成8位数字userid的方法
    @Deprecated // userid字段已删除
    private String generateUserId(){
        Random random = new Random();
        StringBuilder sb = new StringBuilder();
        // 循环生成8位数字
        for (int i = 0; i < 8; i++) {
            int digit = random.nextInt(10); // 生成0到9之间的随机数字
            sb.append(digit);
        }
        return sb.toString();
    }

    @Override
    public User register(UserDTO userDTO) {
        // 检查用户名是否已存在
        if (userMapper.findByUsername(userDTO.getUsername()) != null) {
            throw new RuntimeException("用户名已存在");
        }
        
        // 验证密码强度
        String passwordValidationMessage = PasswordValidator.getPasswordValidationMessage(userDTO.getPassword());
        if (passwordValidationMessage != null) {
            throw new RuntimeException(passwordValidationMessage);
        }
        
        // 创建用户实体
        User user = new User();
        user.setUsername(userDTO.getUsername());
        user.setPassword(encryptPassword(userDTO.getPassword()));
        user.setEmail(userDTO.getEmail());
        user.setPhone(userDTO.getPhone());
        user.setStatus(1);
        user.setCreateTime(LocalDateTime.now());
        user.setUpdateTime(LocalDateTime.now());
        
        // 保存用户
        userMapper.insert(user);
        return user;
    }
    
    @Override
    public User getUserById(Long id) {
        return userMapper.findById(id);
    }
    
    @Override
    public User updateUser(UserDTO userDTO) {
        User user = userMapper.findById(userDTO.getId());
        if (user == null) {
            throw new RuntimeException("用户不存在");
        }
        
        // 更新用户信息
        user.setEmail(userDTO.getEmail());
        user.setPhone(userDTO.getPhone());
        user.setStatus(userDTO.getStatus());
        user.setUpdateTime(LocalDateTime.now());
        
        // 如果密码不为空，则验证并更新密码
        if (userDTO.getPassword() != null && !userDTO.getPassword().isEmpty()) {
            String passwordValidationMessage = PasswordValidator.getPasswordValidationMessage(userDTO.getPassword());
            if (passwordValidationMessage != null) {
                throw new RuntimeException(passwordValidationMessage);
            }
            user.setPassword(encryptPassword(userDTO.getPassword()));
        }
        
        userMapper.update(user);
        return user;
    }
    
    @Override
    public void deleteUser(Long id) {
        userMapper.deleteById(id);
    }
    
    @Override
    public boolean verifyPassword(String rawPassword, String encodedPassword) {
        return encryptPassword(rawPassword).equals(encodedPassword);
    }
}