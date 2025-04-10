package org.swu.vehiclecloud.util;

import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.EnableAspectJAutoProxy;
import org.springframework.web.servlet.config.annotation.ContentNegotiationConfigurer;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
@EnableAspectJAutoProxy // 启用AOP切面支持
public class CorsConfig implements WebMvcConfigurer {

    @Override
    public void addCorsMappings(CorsRegistry registry) {
        registry.addMapping("/**")
                // 允许所有来源
                .allowedOriginPatterns("*")
                // 允许所有方法
                .allowedMethods("*")
                // 允许所有头
                .allowedHeaders("*")
                // 暴露所有头
                .exposedHeaders("*")
                // 不允许凭证
                .allowCredentials(false)
                // 预检请求缓存时间
                .maxAge(3600);
    }

    @Override
    public void configureContentNegotiation(ContentNegotiationConfigurer configurer) {
        configurer.defaultContentType(org.springframework.http.MediaType.APPLICATION_JSON); // 设置默认返回类型为 JSON
    }
}
