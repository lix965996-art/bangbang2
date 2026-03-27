package com.farmland.intel.config;

import org.springframework.beans.BeansException;
import org.springframework.beans.factory.config.BeanPostProcessor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.util.ReflectionUtils;
import org.springframework.web.servlet.mvc.method.RequestMappingInfoHandlerMapping;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;
import springfox.documentation.builders.ApiInfoBuilder;
import springfox.documentation.builders.PathSelectors;
import springfox.documentation.builders.RequestHandlerSelectors;
import springfox.documentation.oas.annotations.EnableOpenApi;
import springfox.documentation.service.ApiInfo;
import springfox.documentation.service.Contact;
import springfox.documentation.spi.DocumentationType;
import springfox.documentation.spring.web.plugins.Docket;
import springfox.documentation.spring.web.plugins.WebFluxRequestHandlerProvider;
import springfox.documentation.spring.web.plugins.WebMvcRequestHandlerProvider;

import java.lang.reflect.Field;
import java.util.List;
import java.util.stream.Collectors;

@Configuration
@EnableOpenApi
public class SwaggerConfig implements WebMvcConfigurer {


    @Bean
    public Docket createRestAPI() {
        return new Docket(DocumentationType.OAS_30)
                .apiInfo(apiInfo())
                .select()
                .apis(RequestHandlerSelectors.basePackage("com.farmland.intel.controller"))
                .paths(PathSelectors.any())
                .build();
    }

    @Bean
    public static BeanPostProcessor springfoxHandlerProviderBeanPostProcessor() {
        return new BeanPostProcessor() {
            @Override
            public Object postProcessAfterInitialization(Object bean, String beanName) throws BeansException {
                if (bean instanceof WebMvcRequestHandlerProvider || bean instanceof WebFluxRequestHandlerProvider) {
                    customizeSpringfoxHandlerMappings(getHandlerMappings(bean));
                }
                return bean;
            }

            private void customizeSpringfoxHandlerMappings(List<RequestMappingInfoHandlerMapping> mappings) {
                List<RequestMappingInfoHandlerMapping> compatibleMappings = mappings.stream()
                        .filter(mapping -> mapping.getPatternParser() == null)
                        .collect(Collectors.toList());
                mappings.clear();
                mappings.addAll(compatibleMappings);
            }

            @SuppressWarnings("unchecked")
            private List<RequestMappingInfoHandlerMapping> getHandlerMappings(Object bean) {
                Field field = ReflectionUtils.findField(bean.getClass(), "handlerMappings");
                if (field == null) {
                    throw new IllegalStateException("handlerMappings field not found in " + bean.getClass());
                }
                ReflectionUtils.makeAccessible(field);
                return (List<RequestMappingInfoHandlerMapping>) ReflectionUtils.getField(field, bean);
            }
        };
    }

    private ApiInfo apiInfo() {
        return new ApiInfoBuilder()
                .title("接口文档")
                .description("Restful 接口")
                .contact(new Contact("xxx", "#", "xxx@qq.com"))
                .version("1.0")
                .build();
    }

}
