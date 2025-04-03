package org.swu.vehiclecloud.util;

import cn.hutool.core.util.StrUtil;
import jakarta.servlet.*;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import jakarta.servlet.annotation.WebFilter;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.swu.vehiclecloud.controller.template.ApiResult;

import java.io.IOException;
import java.util.ArrayList;

@WebFilter
public class JWTAuthFilter implements Filter{
    private final JwtTokenProvider jwtTokenProvider;

    public JWTAuthFilter(JwtTokenProvider jwtTokenProvider) {
        this.jwtTokenProvider = jwtTokenProvider;
    }

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain) throws IOException, ServletException {
        // 类型转换，将 ServletRequest 和 ServletResponse 转换为 HttpServletRequest 和 HttpServletResponse
        HttpServletRequest httpRequest = (HttpServletRequest) request;
        HttpServletResponse httpResponse = (HttpServletResponse) response;

        // 获取请求路径
        String requestURI = httpRequest.getRequestURI();

        // 排除登录接口，登录不需要进行JWT认证
        if (requestURI.equals("/api/usermanage/public/login")) {
            // 不进行拦截，直接传递请求
            chain.doFilter(request, response);
            return;
        }

        // 从请求头获取 JWT Token
        String token = httpRequest.getHeader("Authorization");
        if (token != null && token.startsWith("Bearer ")) {
            try {
                token = token.substring(7);  // 移除 "Bearer " 部分

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
                httpResponse.sendError(HttpServletResponse.SC_UNAUTHORIZED, "Invalid JWT token");
            }
        }
        chain.doFilter(request, response);
    }
}
