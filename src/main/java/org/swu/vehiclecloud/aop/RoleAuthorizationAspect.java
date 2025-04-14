package org.swu.vehiclecloud.aop;

import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Before;
import org.aspectj.lang.annotation.Pointcut;
import org.swu.vehiclecloud.annotations.PreAuthorizeRole;
import org.swu.vehiclecloud.entity.User;
import org.swu.vehiclecloud.exception.AccessDeniedException;
import org.swu.vehiclecloud.mapper.UserMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.swu.vehiclecloud.util.CurrentUserContext;



@Aspect
@Component
public class RoleAuthorizationAspect {

    @Autowired
    private UserMapper userMapper;  // 用于从服务中获取当前用户的角色

    // 定义切点，拦截所有带有 @PreAuthorizeRole 注解的方法
    @Pointcut("@annotation(preAuthorizeRole)")
    public void roleCheckPointcut(PreAuthorizeRole preAuthorizeRole) {}

    // 切面逻辑：执行前进行权限检查
    @Before(value = "roleCheckPointcut(preAuthorizeRole)", argNames = "preAuthorizeRole")
    public void checkRole(PreAuthorizeRole preAuthorizeRole) throws Exception {
        // 获取当前用户的id
        String userid = CurrentUserContext.getCurrentUserId();

        // 根据用户id获取用户角色
        String currentUserRole = getCurrentUserRole(userid);

        // 检查当前用户角色是否符合注解中的角色要求
        boolean hasPermission = false;
        for (String role : preAuthorizeRole.roles()) {
            if (role.equals(currentUserRole)) {
                hasPermission = true;
                break;
            }
        }

        // 如果用户没有权限，则抛出异常
        if (!hasPermission) {
            throw new AccessDeniedException("Access Denied: Insufficient Permissions");
        }
    }

    // 根据用户id获取用户角色
    private String getCurrentUserRole(String userid) {
        int id = Integer.parseInt(userid);
        User user = userMapper.findById(id);
        return user.getRole();
    }
}
