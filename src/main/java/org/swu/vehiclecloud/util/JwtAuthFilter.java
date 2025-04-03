package org.swu.vehiclecloud.util;

import cn.hutool.core.util.StrUtil;
import jakarta.servlet.*;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import jakarta.servlet.annotation.WebFilter;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.swu.vehiclecloud.exception.JwtIsExpiredException;
import org.swu.vehiclecloud.exception.JwtParseFailedException;

import java.io.IOException;
import java.util.ArrayList;

@WebFilter(asyncSupported = true)
public class JwtAuthFilter implements Filter{
    private final JwtTokenProvider jwtTokenProvider;

    public JwtAuthFilter(JwtTokenProvider jwtTokenProvider) {
        this.jwtTokenProvider = jwtTokenProvider;
    }

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain) throws IOException, ServletException {
        // 类型转换，将 ServletRequest 和 ServletResponse 转换为 HttpServletRequest 和 HttpServletResponse
        HttpServletRequest httpRequest = (HttpServletRequest) request;
        HttpServletResponse httpResponse = (HttpServletResponse) response;

        // 获取请求路径
        String requestURI = httpRequest.getRequestURI();

        // 排除登录和注册接口，登录不需要进行JWT认证
        if (requestURI.equals("/api/usermanage/public/login")
        || requestURI.equals("/api/usermanage/public/register")) {
            // 不进行拦截，直接传递请求
            chain.doFilter(request, response);
            return;
        }

        // 从请求头获取 JWT Token
        String token = httpRequest.getHeader("Authorization");
        if (token != null && token.startsWith("Bearer ")) {
            try {
                // 移除 "Bearer " 部分
                token = token.substring(7);

                // 验证 JWT是否过期
                boolean expired = jwtTokenProvider.validateTokenExpiration(token);

                // 如果已过期，则返回给前端401 Unauthorized，让其重定向到登录界面
                if (!expired) {
                    throw new JwtIsExpiredException("Jwt Token is expired");
                }

                // 使用自定义JwtTokenProvider的方法解析用户ID
                String userId = jwtTokenProvider.getUserIdFromToken(token);

                // 如果解析成功，将用户信息放到 SecurityContext 中
                if (!StrUtil.isEmptyIfStr(userId)) {
                    UsernamePasswordAuthenticationToken authentication =
                            new UsernamePasswordAuthenticationToken(userId, null, new ArrayList<>());
                    authentication.setDetails(new WebAuthenticationDetailsSource().buildDetails(httpRequest));

                    // 将认证信息设置到 SecurityContext 中
                    SecurityContextHolder.getContext().setAuthentication(authentication);
                }
            } catch (Exception e) {
                // 解析失败，返回未授权
                throw new JwtParseFailedException("Jwt parse failed");
            }
        }
        chain.doFilter(request, response);
    }
}
