package org.swu.vehiclecloud.config;

import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.swu.vehiclecloud.util.JWTAuthFilter;
import org.swu.vehiclecloud.util.JwtTokenProvider;

@Configuration
public class FilterConfig {

    @Bean
    public FilterRegistrationBean<JWTAuthFilter> jwtAuthFilter(JwtTokenProvider jwtTokenProvider) {
        FilterRegistrationBean<JWTAuthFilter> registrationBean = new FilterRegistrationBean<>();

        // 注册 JWT 过滤器
        registrationBean.setFilter(new JWTAuthFilter(jwtTokenProvider));

        // 设置过滤器规则
        registrationBean.addUrlPatterns("/api/*");  // 拦截所有 /api/* 的请求

        // 设置名称
        registrationBean.setName("JWTAuthFilter");

        // 设置过滤器执行的顺序，数字越小，优先级越高
        registrationBean.setOrder(1);

        // 设置启动标识
        registrationBean.setEnabled(true);

        // 设置初始化参数
        registrationBean.addInitParameter("enable", "true");

        return registrationBean;
    }
}

