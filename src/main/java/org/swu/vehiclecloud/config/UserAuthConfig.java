package org.swu.vehiclecloud.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.access.AccessDeniedHandler;
import org.swu.vehiclecloud.controller.template.ApiResult;

@Configuration
@EnableMethodSecurity // 启用方法级别的安全控制
public class UserAuthConfig {

    private static final Logger logger = LoggerFactory.getLogger(UserAuthConfig.class);

    @Bean
    SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
                http
                // 禁用 HTTP Basic Authentication
                .httpBasic().disable()
                // 禁用csrf防护
                .csrf().disable()
                // 禁用session管理
                .sessionManagement().disable()
                // 禁用默认的登录页面
                .formLogin().disable()
                // 禁用自动登出功能
                .logout().disable()
                .authorizeHttpRequests(authz -> authz
                        .requestMatchers("/admin/**").hasRole("SYS_ADMIN")   // 系统管理员（SYS_ADMIN）访问 /admin/** 路径
                        .requestMatchers("/business/**").hasRole("BIZ_ADMIN")  // 业务管理员（BIZ_ADMIN）访问 /business/** 路径
                        .requestMatchers("/user/**").hasAnyRole("USER", "SYS_ADMIN", "BIZ_ADMIN") // 普通用户、系统管理员和业务管理员可以访问 /user/** 路径
                        .requestMatchers("/public/**").permitAll()   // 所有人可以访问 /public/** 路径
                        .anyRequest().permitAll()// 其他路径不需要认证
                )
                .exceptionHandling()
                .accessDeniedHandler(new CustomAccessDeniedHandler());  // 配置自定义的 AccessDeniedHandler

        return http.build();
    }

    // 自定义 AccessDeniedHandler
    public static class CustomAccessDeniedHandler implements AccessDeniedHandler {
        @Override
        public void handle(HttpServletRequest request, HttpServletResponse response,
                           AccessDeniedException accessDeniedException) {
            response.setStatus(HttpServletResponse.SC_FORBIDDEN);  // 403 禁止访问
            response.setContentType("application/json");

            // 创建 ApiResult 对象
            ApiResult<Object> apiResult = ApiResult.of(403, "Access Denied. You do not have permission to access this resource.");

            // 使用 ObjectMapper 将 ApiResult 转换为 JSON
            try {
                ObjectMapper objectMapper = new ObjectMapper();
                response.getWriter().write(objectMapper.writeValueAsString(apiResult));  // 将 ApiResult 转换成 JSON 格式返回
            } catch (Exception e) {
                logger.error("An error occurred: {}", e.getMessage(), e);
            }
        }
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }
}

