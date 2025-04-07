package org.swu.vehiclecloud.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.swu.vehiclecloud.util.JwtAuthFilter;
import org.swu.vehiclecloud.util.JwtTokenProvider;

@Configuration
@EnableWebSecurity
public class SecurityConfig {

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http, JwtTokenProvider jwtTokenProvider) throws Exception {
        // 禁用 HTTP Basic Authentication 和 CSRF 防护
        http
                .httpBasic().disable()  // 禁用 HTTP Basic Authentication
                .csrf().disable()  // 禁用 CSRF 防护（如果需要）
                .authorizeHttpRequests(authz -> authz
                        // 这段代码表明登录和注册方法不需要JWT认证，其它所有方法均需要JWT认证
                        .requestMatchers("/api/usermanage/public/register").permitAll()
                        .requestMatchers("/api/usermanage/public/login").permitAll()
                        .anyRequest().authenticated());

        // 添加自定义的 JWT 过滤器
        http.addFilterBefore(new JwtAuthFilter(jwtTokenProvider), UsernamePasswordAuthenticationFilter.class);
        return http.build();
    }
}
