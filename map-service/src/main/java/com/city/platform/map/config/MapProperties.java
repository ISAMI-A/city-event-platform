package com.city.platform.map.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * 腾讯位置服务配置：Key 由环境变量注入，禁止硬编码。
 */
@Data
@ConfigurationProperties(prefix = "city.map")
public class MapProperties {

    private String key = "";

    /**
     * WebServiceAPI 签名校验用的 Secret Key；留空则不传 sig（兼容未开启签名校验的 Key）。
     */
    private String sk = "";

    private String baseUrl = "https://apis.map.qq.com";
    private int timeoutSeconds = 5;

    /**
     * 供管理后台地图展示使用的 Web 端受限 Key（可选）；服务端 WebService Key 永不返回前端。
     */
    private String jsApiKey = "";
}
