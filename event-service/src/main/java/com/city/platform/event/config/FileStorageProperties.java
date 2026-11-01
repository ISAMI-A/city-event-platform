package com.city.platform.event.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * 附件存储配置：只保存相对路径与访问地址，二进制不入库。
 */
@Data
@ConfigurationProperties(prefix = "city.file")
public class FileStorageProperties {

    private String root = "uploads";

    private String accessBaseUrl = "/uploads";

    private long maxSize = 5 * 1024 * 1024L;
}
