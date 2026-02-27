package com.jhddt.config.swagger;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Swagger 配置类
 * 配置 API 文档信息和 JWT 认证
 */
@Configuration
public class SwaggerConfig {

    /**
     * 配置 OpenAPI 文档
     */
    @Bean
    public OpenAPI customOpenAPI() {
        return new OpenAPI()
                // 配置 API 基本信息
                .info(new Info()
                        .title("Reading Pavilion API 文档")
                        .description("智能作文批改系统 RESTful API 接口文档")
                        .version("v1.0.0")
                        .contact(new Contact()
                                .name("JHDDT")
                                .email("zhouchaokun8@outlook.com"))
                        .license(new License()
                                .name("Apache 2.0")
                                .url("https://www.apache.org/licenses/LICENSE-2.0.html")))
                
                // 配置 JWT 认证
                .components(new Components()
                        .addSecuritySchemes("Bearer Token", new SecurityScheme()
                                .type(SecurityScheme.Type.HTTP)
                                .scheme("bearer")
                                .bearerFormat("JWT")
                                .description("请输入 JWT Token，格式：Bearer {token}")))
                
                // 全局应用 JWT 认证
                .addSecurityItem(new SecurityRequirement().addList("Bearer Token"));
    }
}
