package org.swu.vehiclecloud.annotations;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 自定义注解，用于在方法上进行权限检查
 */
@Target(ElementType.METHOD)             // 表示注解作用于方法
@Retention(RetentionPolicy.RUNTIME)    // 注解在运行时可用
public @interface PreAuthorizeRole {
    String[] roles();  // 角色数组，用于指定可以访问该方法的角色
}
