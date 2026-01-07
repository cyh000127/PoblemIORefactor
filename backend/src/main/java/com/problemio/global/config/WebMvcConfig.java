package com.problemio.global.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class WebMvcConfig implements WebMvcConfigurer {

    @Value("${file.upload-dir}")
    private String uploadDir;

    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        // 로컬 파일 업로드 경로를 웹에서 접근 가능하도록 매핑
        // 예: /uploads/abc.jpg -> C:/public/upload/abc.jpg
        registry.addResourceHandler("/uploads/**")
                .addResourceLocations("file:///" + uploadDir + "/");
    }
}