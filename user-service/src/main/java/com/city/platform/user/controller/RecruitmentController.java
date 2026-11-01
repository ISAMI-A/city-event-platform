package com.city.platform.user.controller;

import cn.dev33.satoken.annotation.SaCheckRole;
import com.city.platform.common.core.model.R;
import com.city.platform.common.security.constant.RoleConstants;
import com.city.platform.user.dto.RecruitmentRequest;
import com.city.platform.user.service.RecruitmentService;
import com.city.platform.user.vo.RecruitmentVO;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/recruitments")
@RequiredArgsConstructor
public class RecruitmentController {

    private final RecruitmentService recruitmentService;

    @GetMapping
    @SaCheckRole(RoleConstants.ADMIN)
    public R<List<RecruitmentVO>> adminList() {
        return R.ok(recruitmentService.adminList());
    }

    @PostMapping
    @SaCheckRole(RoleConstants.ADMIN)
    public R<RecruitmentVO> create(@Valid @RequestBody RecruitmentRequest request) {
        return R.ok(recruitmentService.create(request));
    }

    @PutMapping("/{id}")
    @SaCheckRole(RoleConstants.ADMIN)
    public R<RecruitmentVO> update(@PathVariable("id") Long id,
                                   @Valid @RequestBody RecruitmentRequest request) {
        return R.ok(recruitmentService.update(id, request));
    }

    @PutMapping("/{id}/status")
    @SaCheckRole(RoleConstants.ADMIN)
    public R<RecruitmentVO> updateStatus(@PathVariable("id") Long id,
                                         @RequestBody Map<String, String> body) {
        return R.ok(recruitmentService.updateStatus(id, body.get("status")));
    }

    @GetMapping("/{id}/applications")
    @SaCheckRole(RoleConstants.ADMIN)
    public R<List<Map<String, Object>>> applications(@PathVariable("id") Long id) {
        return R.ok(recruitmentService.applications(id));
    }

    @PutMapping("/applications/{applyId}/status")
    @SaCheckRole(RoleConstants.ADMIN)
    public R<Void> updateApplyStatus(@PathVariable("applyId") Long applyId,
                                     @RequestBody Map<String, String> body) {
        recruitmentService.updateApplyStatus(applyId, body.get("status"));
        return R.ok();
    }

    @GetMapping("/open")
    public R<List<RecruitmentVO>> openList() {
        return R.ok(recruitmentService.openList());
    }

    @GetMapping("/{id}")
    public R<RecruitmentVO> detail(@PathVariable("id") Long id) {
        return R.ok(recruitmentService.detail(id));
    }

    @PostMapping("/{id}/apply")
    public R<Map<String, Object>> apply(@PathVariable("id") Long id,
                                        @RequestBody(required = false) Map<String, String> body) {
        String note = body == null ? null : body.get("note");
        return R.ok(recruitmentService.apply(id, note));
    }

    @GetMapping("/my-applications")
    public R<List<Map<String, Object>>> myApplications() {
        return R.ok(recruitmentService.myApplications());
    }
}
