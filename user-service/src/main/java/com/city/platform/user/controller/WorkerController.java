package com.city.platform.user.controller;

import cn.dev33.satoken.annotation.SaCheckRole;
import com.city.platform.common.core.model.R;
import com.city.platform.common.security.constant.RoleConstants;
import com.city.platform.user.dto.WorkerCreateRequest;
import com.city.platform.user.dto.WorkerUpdateRequest;
import com.city.platform.user.service.WorkerManageService;
import com.city.platform.user.vo.WorkerVO;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/workers")
@RequiredArgsConstructor
public class WorkerController {

    private final WorkerManageService workerManageService;

    @GetMapping
    @SaCheckRole(RoleConstants.ADMIN)
    public R<List<WorkerVO>> list(@RequestParam(value = "type", required = false) String type) {
        return R.ok(workerManageService.list(type));
    }

    /**
     * 组长查看自己的组员。
     */
    @GetMapping("/my-team")
    @SaCheckRole(RoleConstants.WORKER)
    public R<List<WorkerVO>> myTeam() {
        return R.ok(workerManageService.myTeam());
    }

    @PostMapping
    @SaCheckRole(RoleConstants.ADMIN)
    public R<WorkerVO> create(@Valid @RequestBody WorkerCreateRequest request) {
        return R.ok(workerManageService.create(request));
    }

    @PutMapping("/{id}")
    @SaCheckRole(RoleConstants.ADMIN)
    public R<WorkerVO> update(@PathVariable("id") Long id,
                              @Valid @RequestBody WorkerUpdateRequest request) {
        return R.ok(workerManageService.update(id, request));
    }

    @GetMapping("/me/profile")
    @SaCheckRole(RoleConstants.WORKER)
    public R<Map<String, Object>> me() {
        return R.ok(workerManageService.currentWorkerProfile());
    }
}
