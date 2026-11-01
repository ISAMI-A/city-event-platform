package com.city.platform.user.controller;

import com.city.platform.common.core.model.R;
import com.city.platform.user.dto.EmailBindRequest;
import com.city.platform.user.dto.EmailCodeRequest;
import com.city.platform.user.dto.LoginRequest;
import com.city.platform.user.dto.PasswordChangeRequest;
import com.city.platform.user.dto.PhoneUpdateRequest;
import com.city.platform.user.dto.ProfileUpdateRequest;
import com.city.platform.user.dto.RegisterRequest;
import com.city.platform.user.dto.UsernameUpdateRequest;
import com.city.platform.user.dto.WxBindRequest;
import com.city.platform.user.dto.WxCodeRequest;
import com.city.platform.user.dto.WxLoginRequest;
import com.city.platform.user.dto.WxRegisterRequest;
import com.city.platform.user.service.AuthService;
import com.city.platform.user.vo.LoginVO;
import com.city.platform.user.vo.WxLoginVO;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.util.StringUtils;

import java.util.Map;

@RestController
@RequestMapping("/api/v1/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;

    @PostMapping("/login")
    public R<LoginVO> login(@Valid @RequestBody LoginRequest request) {
        return R.ok(authService.login(request));
    }

    @PostMapping("/wx-login")
    public R<WxLoginVO> wxLogin(@Valid @RequestBody WxLoginRequest request) {
        return R.ok(authService.wxLogin(request));
    }

    @PostMapping("/wx-bind")
    public R<WxLoginVO> wxBind(@Valid @RequestBody WxBindRequest request) {
        return R.ok(authService.wxBind(request));
    }

    @PostMapping("/wx-register")
    public R<WxLoginVO> wxRegister(@Valid @RequestBody WxRegisterRequest request) {
        return R.ok(authService.wxRegister(request));
    }

    /**
     * 居民自助注册：用户名 + 密码 + 昵称 + 手机号，成功后直接返回 token。
     */
    @PostMapping("/register")
    public R<LoginVO> register(@Valid @RequestBody RegisterRequest request) {
        return R.ok(authService.register(request));
    }

    /**
     * 已登录账号绑定微信（小程序 wx.login 的 code）。
     */
    @PostMapping("/wx/bind")
    public R<Map<String, Object>> bindWechat(@Valid @RequestBody WxCodeRequest request) {
        return R.ok(authService.bindWechat(request.getCode()));
    }

    /**
     * 已登录账号解绑微信。
     */
    @PostMapping("/wx/unbind")
    public R<Map<String, Object>> unbindWechat() {
        return R.ok(authService.unbindWechat());
    }

    @PutMapping("/me/profile")
    public R<Map<String, Object>> updateProfile(@Valid @RequestBody ProfileUpdateRequest request) {
        return R.ok(authService.updateProfile(request));
    }

    /**
     * 修改本人登录账号（唯一）；已设置密码的账号需带当前登录密码。
     */
    @PutMapping("/me/username")
    public R<Map<String, Object>> updateUsername(@Valid @RequestBody UsernameUpdateRequest request) {
        return R.ok(authService.updateUsername(request.getUsername(), request.getPassword()));
    }

    /**
     * 绑定 / 更换本人手机号；更换已绑定手机号需带当前登录密码。
     */
    @PutMapping("/me/phone")
    public R<Map<String, Object>> updatePhone(@Valid @RequestBody PhoneUpdateRequest request) {
        return R.ok(authService.updatePhone(request.getPhone(), request.getPassword()));
    }

    /**
     * 设置 / 修改本人登录密码。
     */
    @PutMapping("/me/password")
    public R<Map<String, Object>> changePassword(@Valid @RequestBody PasswordChangeRequest request) {
        return R.ok(authService.changePassword(request.getOldPassword(), request.getNewPassword()));
    }

    /**
     * 发送邮箱验证码（5 分钟内有效，60 秒冷却）。
     */
    @PostMapping("/email/code")
    public R<Void> sendEmailCode(@Valid @RequestBody EmailCodeRequest request, HttpServletRequest httpRequest) {
        authService.sendEmailCode(request.getEmail(), clientIp(httpRequest));
        return R.ok();
    }

    /**
     * 校验验证码并绑定 / 更换邮箱；更换已绑定邮箱需带当前登录密码。
     */
    @PostMapping("/email/bind")
    public R<Map<String, Object>> bindEmail(@Valid @RequestBody EmailBindRequest request) {
        return R.ok(authService.bindEmail(request.getEmail(), request.getCode(), request.getPassword()));
    }

    /**
     * 解绑当前账号的邮箱。
     */
    @PostMapping("/email/unbind")
    public R<Map<String, Object>> unbindEmail() {
        return R.ok(authService.unbindEmail());
    }

    @PostMapping("/logout")
    public R<Void> logout() {
        authService.logout();
        return R.ok();
    }

    @GetMapping("/me")
    public R<Map<String, Object>> me() {
        return R.ok(authService.currentUser());
    }

    private String clientIp(HttpServletRequest request) {
        String forwarded = request.getHeader("X-Forwarded-For");
        if (StringUtils.hasText(forwarded)) {
            return forwarded.split(",")[0].trim();
        }
        return request.getRemoteAddr();
    }
}
