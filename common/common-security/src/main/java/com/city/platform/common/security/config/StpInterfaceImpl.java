package com.city.platform.common.security.config;

import cn.dev33.satoken.session.SaSession;
import cn.dev33.satoken.stp.StpInterface;
import cn.dev33.satoken.stp.StpUtil;

import java.util.Collections;
import java.util.List;

/**
 * 角色数据源：登录时写入 SaSession，注解鉴权时读取。
 */
public class StpInterfaceImpl implements StpInterface {

    @Override
    public List<String> getPermissionList(Object loginId, String loginType) {
        return Collections.emptyList();
    }

    @Override
    public List<String> getRoleList(Object loginId, String loginType) {
        SaSession session = StpUtil.getSessionByLoginId(loginId, false);
        if (session == null) {
            return Collections.emptyList();
        }
        Object roles = session.get("roles");
        if (roles instanceof List<?> list && !list.isEmpty()) {
            return list.stream().map(String::valueOf).toList();
        }
        Object activeRole = session.get("activeRole");
        return activeRole == null ? Collections.emptyList() : List.of(String.valueOf(activeRole));
    }
}
