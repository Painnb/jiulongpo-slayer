package org.swu.vehiclecloud.service;

import org.swu.vehiclecloud.controller.template.ApiResult;
import org.swu.vehiclecloud.entity.User;
import java.util.Map;
import org.springframework.transaction.annotation.Transactional;

/**
 * 用户服务接口
 * 定义用户相关的业务操作
 */
@Transactional
public interface UserService {
    // 登录接口
    ApiResult<Map<String, Object>> login(Map<String, Object> requestBody);

    /**
     * 用户注册
     * @param user 用户DTO对象
     * @return 注册后的用户对象
     */
    User register(User user);

    /**
     * 根据用户ID获取用户
     * @param id 用户ID
     * @return 用户对象
     */
    User getUserById(Integer id);

    /**
     * 更新用户信息
     * @param user 用户DTO对象
     * @return 更新后的用户对象
     */
    User updateUser(User user);

    /**
     * 删除用户
     * @param id 用户ID
     */
    void deleteUser(Integer id);

    /**
     * 验证密码
     * @param rawPassword 原始密码
     * @param encodedPassword 编码后的密码
     * @return 验证结果
     */
    boolean verifyPassword(String rawPassword, String encodedPassword);
}