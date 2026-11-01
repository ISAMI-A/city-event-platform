package com.city.platform.common.security.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * 内部接口共享密钥：必须通过环境变量 INTERNAL_TOKEN 注入，禁止提交到仓库。
 */
@Data
@ConfigurationProperties(prefix = "city.security")
public class InternalTokenProperties {

    private String internalToken = "";
}
