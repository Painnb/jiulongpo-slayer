package org.swu.vehiclecloud.util;

import cn.hutool.core.util.StrUtil;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

public class CurrentUserContext {

    // 获取SecurityContext中的用户信息并解析出用户id
    public static String getCurrentUserId() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (!StrUtil.isEmptyIfStr(authentication)) {
            // 获取当前认证用户的id
            return authentication.getName();
        }
        return null;
    }

    // 清除SecurityContext中的用户信息
    public static void clear() {
        SecurityContextHolder.clearContext();
    }
}

