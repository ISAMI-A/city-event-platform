package com.city.platform.user.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * 微信小程序配置：AppSecret 由环境变量注入，禁止硬编码。
 */
@Data
@ConfigurationProperties(prefix = "city.wechat")
public class WechatProperties {

    private String appId = "";

    private String appSecret = "";

    private String baseUrl = "https://api.weixin.qq.com";

    private int timeoutSeconds = 5;

    /**
     * 仅本地开发可开启：允许 code 以 mock: 前缀走受控模拟登录，生产必须关闭。
     */
    private boolean mockEnabled = false;
}
