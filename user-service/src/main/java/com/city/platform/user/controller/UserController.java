package com.city.platform.user.controller;

import cn.dev33.satoken.annotation.SaCheckRole;
import com.city.platform.common.core.model.PageQuery;
import com.city.platform.common.core.model.PageResult;
import com.city.platform.common.core.model.R;
import com.city.platform.common.security.constant.RoleConstants;
import com.city.platform.user.dto.ResetPasswordRequest;
import com.city.platform.user.dto.UserRolesRequest;
import com.city.platform.user.dto.UserStatusRequest;
import com.city.platform.user.service.UserManageService;
import com.city.platform.user.vo.UserVO;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1")
@RequiredArgsConstructor
public class UserController {

    private final UserManageService userManageService;

    @GetMapping("/users")
    @SaCheckRole(RoleConstants.ADMIN)
    public R<PageResult<UserVO>> page(@Valid PageQuery query,
                                      @RequestParam(required = false) String keyword,
                                      @RequestParam(required = false) String status,
                                      @RequestParam(required = false) String role) {
        return R.ok(userManageService.page(query, keyword, status, role));
    }

    @GetMapping("/users/{id}")
    @SaCheckRole(RoleConstants.ADMIN)
    public R<UserVO> detail(@PathVariable("id") Long id) {
        return R.ok(userManageService.detail(id));
    }

    @PatchMapping("/users/{id}/status")
    @SaCheckRole(RoleConstants.ADMIN)
    public R<UserVO> updateStatus(@PathVariable("id") Long id,
                                  @Valid @RequestBody UserStatusRequest request) {
        return R.ok(userManageService.updateStatus(id, request));
    }

    @PatchMapping("/users/{id}/roles")
    @SaCheckRole(RoleConstants.ADMIN)
    public R<UserVO> updateRoles(@PathVariable("id") Long id,
                                 @Valid @RequestBody UserRolesRequest request) {
        return R.ok(userManageService.updateRoles(id, request));
    }

    @PostMapping("/users/{id}/reset-password")
    @SaCheckRole(RoleConstants.ADMIN)
    public R<Void> resetPassword(@PathVariable("id") Long id,
                                 @Valid @RequestBody ResetPasswordRequest request) {
        userManageService.resetPassword(id, request);
        return R.ok();
    }
}
