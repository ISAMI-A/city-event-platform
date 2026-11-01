package com.city.platform.user.vo;

import lombok.Builder;
import lombok.Data;

import java.util.List;

@Data
@Builder
public class WxLoginVO {

    private String token;
    private Long userId;
    private String nickname;
    private String avatarUrl;
    private String activeRole;
    private List<String> roles;
    private long expireSeconds;

    /**
     * true 表示该微信尚未绑定账号，前端需引导绑定工人账号或注册居民账号。
     */
    private boolean needBind;

    /**
     * 未绑定状态下的一次性凭证，5 分钟内有效，用于 wx-bind / wx-register。
     */
    private String bindTicket;

    public static WxLoginVO pending(String bindTicket) {
        return WxLoginVO.builder().needBind(true).bindTicket(bindTicket).build();
    }
}
