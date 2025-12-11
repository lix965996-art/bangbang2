package com.farmland.intel.config;

import com.farmland.intel.config.interceptor.JwtInterceptor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class InterceptorConfig implements WebMvcConfigurer {

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(jwtInterceptor())
                .addPathPatterns("/**")
                // 用户认证相关
                .excludePathPatterns("/user/login", "/user/register")
                // 文件操作
                .excludePathPatterns("/**/export", "/**/import", "/file/**")
                // 业务模块（根据需要调整）
                .excludePathPatterns("/inventory/**", "/purchase/**", "/sales/**", "/order/**", "/onlineSale/**")
                // Swagger文档
                .excludePathPatterns("/swagger**/**", "/webjars/**", "/v3/**", "/doc.html")
                // 物联网和天气服务
                .excludePathPatterns("/aether/**", "/amap/**")
                // AI聊天和果蔬检测
                .excludePathPatterns("/api/chat/**", "/fruit-detect/**")
                // 健康检查和农情日报
                .excludePathPatterns("/health", "/actuator/**", "/agri-report/**");
    }

    @Bean
    public JwtInterceptor jwtInterceptor() {
        return new JwtInterceptor();
    }

}
