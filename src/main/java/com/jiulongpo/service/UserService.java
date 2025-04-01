package com.jiulongpo.service;

import com.jiulongpo.dto.RegisterRequest;
import com.jiulongpo.exception.BusinessException;

/**
 * 用户服务接口
 * 定义用户相关的业务操作
 */
public interface UserService {

    /**
     * 用户注册
     * @param request 注册请求对象
     * @throws BusinessException 当用户名已存在或注册失败时抛出
     */
    void register(RegisterRequest request) throws BusinessException;
}