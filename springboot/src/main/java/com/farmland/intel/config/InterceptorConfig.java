package com.farmland.intel.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * 拦截器配置类
 * JWT拦截器已禁用用于开发测试
 */
@Configuration
public class InterceptorConfig implements WebMvcConfigurer {

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        // 暂时禁用JWT拦截器，用于开发测试
        // registry.addInterceptor(jwtInterceptor())
        //         .addPathPatterns("/**")
        //         // 用户认证相关
        //         .excludePathPatterns("/user/login", "/user/register")
        //         // 文件操作
        //         .excludePathPatterns("/**/export", "/**/import", "/file/**")
        //         // 业务模块（根据需要调整）
        //         .excludePathPatterns("/inventory/**", "/purchase/**", "/sales/**", "/order/**", "/onlineSale/**")
        //         // Swagger文档
        //         .excludePathPatterns("/swagger**/**", "/webjars/**", "/v3/**", "/doc.html")
        //         // 物联网和天气服务
        //         .excludePathPatterns("/aether/**", "/amap/**")
        //         // AI聊天、Agent和果蔬检测
        //         .excludePathPatterns("/api/chat/**", "/api/agent/**", "/fruit-detect/**")
        //         // 健康检查和农情日报
        //         .excludePathPatterns("/health", "/actuator/**", "/agri-report/**");
    }

    // @Bean
    // public JwtInterceptor jwtInterceptor() {
    //     return new JwtInterceptor();
    // }

}
