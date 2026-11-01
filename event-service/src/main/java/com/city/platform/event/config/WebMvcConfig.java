package com.city.platform.event.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import java.nio.file.Paths;

/**
 * dev/local 环境直接暴露上传目录；生产由 Nginx 托管同一目录。
 */
@Configuration
public class WebMvcConfig implements WebMvcConfigurer {

    private final FileStorageProperties properties;

    public WebMvcConfig(FileStorageProperties properties) {
        this.properties = properties;
    }

    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        String location = Paths.get(properties.getRoot()).toAbsolutePath().toUri().toString();
        registry.addResourceHandler(properties.getAccessBaseUrl() + "/**")
                .addResourceLocations(location);
    }
}
