package com.jiulongpo.service.impl;

import com.jiulongpo.dto.RegisterRequest;
import com.jiulongpo.entity.User;
import com.jiulongpo.exception.BusinessException;
import com.jiulongpo.mapper.UserMapper;
import com.jiulongpo.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 用户服务实现类
 */
@Service
public class UserServiceImpl implements UserService {

    @Autowired
    private UserMapper userMapper;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Override
    @Transactional
    public void register(RegisterRequest request) throws BusinessException {
        // 检查用户名是否已存在
        if (userMapper.selectByUsername(request.getUsername()) != null) {
            throw new BusinessException("用户名已存在");
        }

        // 创建用户实体
        User user = new User();
        user.setUsername(request.getUsername());
        // 加密密码
        user.setPassword(passwordEncoder.encode(request.getPassword()));

        // 保存用户
        if (userMapper.insert(user) != 1) {
            throw new BusinessException("注册失败");
        }
    }
}