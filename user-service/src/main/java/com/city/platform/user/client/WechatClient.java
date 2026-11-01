package com.city.platform.user.client;

import com.city.platform.common.core.exception.BizException;
import com.city.platform.common.core.exception.ErrorCode;
import com.city.platform.user.config.WechatProperties;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.util.DigestUtils;
import org.springframework.util.StringUtils;
import org.springframework.web.client.RestClient;

import java.nio.charset.StandardCharsets;

/**
 * 微信小程序登录凭据交换：code2session。
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class WechatClient {

    private static final String MOCK_PREFIX = "mock:";

    private final RestClient wechatRestClient;
    private final WechatProperties properties;
    private final ObjectMapper objectMapper;

    public WxSession code2session(String code) {
        if (!StringUtils.hasText(code)) {
            throw new BizException(ErrorCode.PARAM_INVALID, "微信登录凭证不能为空");
        }
        if (code.startsWith(MOCK_PREFIX)) {
            if (!properties.isMockEnabled()) {
                throw new BizException(ErrorCode.WECHAT_LOGIN_FAILED, "微信 mock 登录未开启");
            }
            String openId = "mock_" + DigestUtils.md5DigestAsHex(code.getBytes(StandardCharsets.UTF_8));
            log.info("微信登录走 mock 模式 openId={}", openId);
            return new WxSession(openId, null);
        }
        if (!StringUtils.hasText(properties.getAppId()) || !StringUtils.hasText(properties.getAppSecret())) {
            throw new BizException(ErrorCode.WECHAT_LOGIN_FAILED, "服务端未配置微信小程序凭据");
        }
        // 微信接口偶发返回 text/plain，按字符串读取再解析，避免内容类型不匹配导致反序列化失败
        String raw;
        try {
            raw = wechatRestClient.get()
                    .uri(uriBuilder -> uriBuilder.path("/sns/jscode2session")
                            .queryParam("appid", properties.getAppId())
                            .queryParam("secret", properties.getAppSecret())
                            .queryParam("js_code", code)
                            .queryParam("grant_type", "authorization_code")
                            .build())
                    .retrieve()
                    .body(String.class);
        } catch (Exception e) {
            log.error("调用微信 code2session 异常", e);
            throw new BizException(ErrorCode.SERVICE_UNAVAILABLE, "微信服务暂不可用，请稍后重试");
        }
        if (!StringUtils.hasText(raw)) {
            throw new BizException(ErrorCode.WECHAT_LOGIN_FAILED, "微信服务返回空响应");
        }
        JsonNode response;
        try {
            response = objectMapper.readTree(raw);
        } catch (Exception e) {
            log.warn("微信 code2session 响应解析失败 raw={}", raw);
            throw new BizException(ErrorCode.WECHAT_LOGIN_FAILED);
        }
        String openId = response.path("openid").asText("");
        if (!StringUtils.hasText(openId)) {
            int errCode = response.path("errcode").asInt(-1);
            String errMsg = response.path("errmsg").asText("");
            log.warn("微信 code2session 失败 errcode={} errmsg={}", errCode, errMsg);
            throw new BizException(ErrorCode.WECHAT_LOGIN_FAILED);
        }
        String unionId = response.path("unionid").asText("");
        return new WxSession(openId, StringUtils.hasText(unionId) ? unionId : null);
    }

    public record WxSession(String openId, String unionId) {
    }
}
