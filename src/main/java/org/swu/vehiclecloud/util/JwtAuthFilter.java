package org.swu.vehiclecloud.util;

import cn.hutool.core.util.StrUtil;
import jakarta.servlet.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import jakarta.servlet.annotation.WebFilter;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;
import java.util.ArrayList;

@WebFilter(asyncSupported = true)
public class JwtAuthFilter implements Filter {
    private static final Logger logger = LoggerFactory.getLogger(JwtAuthFilter.class);

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
        logger.debug("处理请求: {}", requestURI);

        // 排除登录和注册接口，登录不需要进行JWT认证
        if (requestURI.equals("/api/usermanage/public/login")
                || requestURI.equals("/api/usermanage/public/register")) {
            // 不进行拦截，直接传递请求
            logger.debug("公开路径，跳过认证: {}", requestURI);
            chain.doFilter(request, response);
            return;
        }

        try {
            // 从请求头获取 JWT Token
            String token = httpRequest.getHeader("Authorization");
            logger.debug("收到的Authorization头: {}", token);

            if (token != null && token.startsWith("Bearer ")) {
                // 移除 "Bearer " 部分
                token = token.substring(7);
                logger.debug("处理后的令牌: {}", token);

                // 验证 JWT是否过期
                boolean expired = jwtTokenProvider.validateTokenExpiration(token);

                // 如果已过期，则返回给前端401 Unauthorized，让其重定向到登录界面
                if (!expired) {
                    logger.warn("令牌已过期");
                    httpResponse.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
                    httpResponse.getWriter().write("{\"message\":\"Token has expired\",\"status\":401}");
                    return;
                }

                // 使用自定义JwtTokenProvider的方法解析用户ID
                String userId = jwtTokenProvider.getUserIdFromToken(token);
                logger.debug("解析的用户ID: {}", userId);

                // 如果解析成功，将用户信息放到 SecurityContext 中
                if (!StrUtil.isEmptyIfStr(userId)) {
                    UsernamePasswordAuthenticationToken authentication =
                            new UsernamePasswordAuthenticationToken(userId, null, new ArrayList<>());
                    authentication.setDetails(new WebAuthenticationDetailsSource().buildDetails(httpRequest));

                    // 将认证信息设置到 SecurityContext 中
                    SecurityContextHolder.getContext().setAuthentication(authentication);
                }
            } else {
                // 如果没有令牌或格式不正确，返回401状态码
                if (requestURI.startsWith("/api/") && !requestURI.contains("/public/")) {
                    logger.warn("请求需要认证但没有提供有效令牌: {}", requestURI);
                    httpResponse.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
                    httpResponse.getWriter().write("{\"message\":\"Authorization required\",\"status\":401}");
                    return;
                }
            }

            // 继续过滤链
            chain.doFilter(request, response);

        } catch (Exception e) {
            logger.error("处理JWT认证时发生错误: {}", e.getMessage(), e);

            // 返回适当的错误响应，而不是抛出异常
            httpResponse.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            httpResponse.getWriter().write("{\"message\":\"Authentication failed\",\"status\":401}");
        }
    }
}
