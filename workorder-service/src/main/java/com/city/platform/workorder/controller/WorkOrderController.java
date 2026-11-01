package com.city.platform.workorder.controller;

import cn.dev33.satoken.annotation.SaCheckRole;
import com.city.platform.common.api.dto.DraftGenerateResult;
import com.city.platform.common.api.dto.RectifyVerifyResult;
import com.city.platform.common.core.model.PageResult;
import com.city.platform.common.core.model.R;
import com.city.platform.common.security.constant.RoleConstants;
import com.city.platform.common.security.context.UserContext;
import com.city.platform.workorder.dto.AcceptanceRequest;
import com.city.platform.workorder.dto.AnalyzeDraftRequest;
import com.city.platform.workorder.dto.AssignRequest;
import com.city.platform.workorder.dto.DraftRequest;
import com.city.platform.workorder.dto.LeaderReviewRequest;
import com.city.platform.workorder.dto.ProcessRequest;
import com.city.platform.workorder.dto.RatingRequest;
import com.city.platform.workorder.dto.ReassignRequest;
import com.city.platform.workorder.dto.SubmitRequest;
import com.city.platform.workorder.dto.WorkOrderQuery;
import com.city.platform.workorder.service.AiVerifyService;
import com.city.platform.workorder.service.WorkOrderService;
import com.city.platform.workorder.vo.WorkOrderDetailVO;
import com.city.platform.workorder.vo.WorkOrderListItemVO;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.DeleteMapping;
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
@RequestMapping("/api/v1/workorders")
@RequiredArgsConstructor
public class WorkOrderController {

    private final WorkOrderService workOrderService;
    private final AiVerifyService aiVerifyService;

    @GetMapping("/my")
    @SaCheckRole(RoleConstants.WORKER)
    public R<PageResult<WorkOrderListItemVO>> myWorkOrders(@Valid WorkOrderQuery query) {
        return R.ok(workOrderService.page(query, UserContext.require()));
    }

    /**
     * 组长查看本组工单。
     */
    @GetMapping("/team")
    @SaCheckRole(RoleConstants.WORKER)
    public R<PageResult<WorkOrderListItemVO>> teamWorkOrders(@Valid WorkOrderQuery query) {
        return R.ok(workOrderService.teamPage(query, UserContext.require()));
    }

    /**
     * 处理人在手工单数，供派单/分配做工作量平衡。
     */
    @GetMapping("/loads")
    public R<List<Map<String, Object>>> loads(@RequestParam("ids") String ids) {
        return R.ok(workOrderService.loads(ids));
    }

    /**
     * 组长查看组内绩效（本月完成数、平均评分、处理时长、退回次数、绩效分）。
     */
    @GetMapping("/team/stats")
    @SaCheckRole(RoleConstants.WORKER)
    public R<List<Map<String, Object>>> teamStats() {
        return R.ok(workOrderService.teamStats(UserContext.require()));
    }

    @GetMapping("/{id}")
    public R<WorkOrderDetailVO> detail(@PathVariable("id") Long id) {
        return R.ok(workOrderService.detail(id, UserContext.require()));
    }

    /**
     * 组长把工单分配给本组组员。
     */
    @PostMapping("/{id}/assign")
    @SaCheckRole(RoleConstants.WORKER)
    public R<WorkOrderDetailVO> assign(@PathVariable("id") Long id,
                                       @Valid @RequestBody AssignRequest request) {
        workOrderService.assign(id, request, UserContext.require());
        return R.ok(workOrderService.detail(id, UserContext.require()));
    }

    /**
     * 组长复核组员提交的结果。
     */
    @PostMapping("/{id}/leader-review")
    @SaCheckRole(RoleConstants.WORKER)
    public R<WorkOrderDetailVO> leaderReview(@PathVariable("id") Long id,
                                             @Valid @RequestBody LeaderReviewRequest request) {
        workOrderService.leaderReview(id, request, UserContext.require());
        return R.ok(workOrderService.detail(id, UserContext.require()));
    }

    /**
     * 居民在工单完成后评价处理人。
     */
    @PostMapping("/{id}/rating")
    public R<WorkOrderDetailVO> rating(@PathVariable("id") Long id,
                                       @Valid @RequestBody RatingRequest request) {
        workOrderService.rate(id, request, UserContext.require());
        return R.ok(workOrderService.detail(id, UserContext.require()));
    }

    @GetMapping
    @SaCheckRole(RoleConstants.ADMIN)
    public R<PageResult<WorkOrderListItemVO>> page(@Valid WorkOrderQuery query) {
        return R.ok(workOrderService.page(query, UserContext.require()));
    }

    @PostMapping("/{id}/accept")
    @SaCheckRole(RoleConstants.WORKER)
    public R<WorkOrderDetailVO> accept(@PathVariable("id") Long id) {
        workOrderService.accept(id, UserContext.require());
        return R.ok(workOrderService.detail(id, UserContext.require()));
    }

    /**
     * 撤销接单：工单退回待接单，可重新分配。
     */
    @PostMapping("/{id}/cancel-accept")
    @SaCheckRole(RoleConstants.WORKER)
    public R<WorkOrderDetailVO> cancelAccept(@PathVariable("id") Long id) {
        workOrderService.cancelAccept(id, UserContext.require());
        return R.ok(workOrderService.detail(id, UserContext.require()));
    }

    /**
     * 撤回已提交的处理结果：工单回到处理中。
     */
    @PostMapping("/{id}/withdraw")
    @SaCheckRole(RoleConstants.WORKER)
    public R<WorkOrderDetailVO> withdraw(@PathVariable("id") Long id) {
        workOrderService.withdrawSubmit(id, UserContext.require());
        return R.ok(workOrderService.detail(id, UserContext.require()));
    }

    /**
     * 删除本人提交的过程记录（仅处理中可删）。
     */
    @DeleteMapping("/{id}/logs/{logId}")
    @SaCheckRole(RoleConstants.WORKER)
    public R<WorkOrderDetailVO> deleteLog(@PathVariable("id") Long id,
                                          @PathVariable("logId") Long logId) {
        workOrderService.deleteProcessLog(id, logId, UserContext.require());
        return R.ok(workOrderService.detail(id, UserContext.require()));
    }

    /**
     * AI 按事件描述与整改后照片生成处理说明草稿。
     */
    @PostMapping("/{id}/ai-draft")
    @SaCheckRole(RoleConstants.WORKER)
    public R<DraftGenerateResult> aiDraft(@PathVariable("id") Long id,
                                          @Valid @RequestBody DraftRequest request) {
        return R.ok(aiVerifyService.generateDraft(id, request, UserContext.require()));
    }

    @PostMapping("/{id}/process")
    @SaCheckRole(RoleConstants.WORKER)
    public R<WorkOrderDetailVO> process(@PathVariable("id") Long id,
                                        @Valid @RequestBody ProcessRequest request) {
        workOrderService.addProcess(id, request, UserContext.require());
        return R.ok(workOrderService.detail(id, UserContext.require()));
    }

    /**
     * 过程记录自动保存：内容或照片变化时原位更新同一条记录。
     */
    @PutMapping("/{id}/process")
    @SaCheckRole(RoleConstants.WORKER)
    public R<WorkOrderDetailVO> saveProcess(@PathVariable("id") Long id,
                                            @Valid @RequestBody ProcessRequest request) {
        workOrderService.saveProcess(id, request, UserContext.require());
        return R.ok(workOrderService.detail(id, UserContext.require()));
    }

    @PostMapping("/{id}/submit")
    @SaCheckRole(RoleConstants.WORKER)
    public R<WorkOrderDetailVO> submit(@PathVariable("id") Long id,
                                       @Valid @RequestBody SubmitRequest request) {
        workOrderService.submit(id, request, UserContext.require());
        return R.ok(workOrderService.detail(id, UserContext.require()));
    }

    @PostMapping("/{id}/acceptance")
    @SaCheckRole(RoleConstants.ADMIN)
    public R<WorkOrderDetailVO> acceptance(@PathVariable("id") Long id,
                                           @Valid @RequestBody AcceptanceRequest request) {
        workOrderService.acceptance(id, request, UserContext.require());
        return R.ok(workOrderService.detail(id, UserContext.require()));
    }

    @PostMapping("/{id}/ai-verify")
    @SaCheckRole(RoleConstants.ADMIN)
    public R<RectifyVerifyResult> aiVerify(@PathVariable("id") Long id) {
        return R.ok(aiVerifyService.verify(id));
    }

    /**
     * 工人提交前让 AI 预检当前草稿，结果仅供工人自查，不写入工单。
     */
    @PostMapping("/{id}/ai-analyze")
    @SaCheckRole(RoleConstants.WORKER)
    public R<RectifyVerifyResult> aiAnalyze(@PathVariable("id") Long id,
                                            @Valid @RequestBody AnalyzeDraftRequest request) {
        return R.ok(aiVerifyService.analyzeDraft(id, request, UserContext.require()));
    }

    @PostMapping("/{id}/reassign")
    @SaCheckRole(RoleConstants.ADMIN)
    public R<WorkOrderDetailVO> reassign(@PathVariable("id") Long id,
                                         @Valid @RequestBody ReassignRequest request) {
        workOrderService.reassign(id, request, UserContext.require());
        return R.ok(workOrderService.detail(id, UserContext.require()));
    }

    /**
     * 管理员删除工单：仅待接单可删，删除后事件退回待审核。
     */
    @DeleteMapping("/{id}")
    @SaCheckRole(RoleConstants.ADMIN)
    public R<Void> delete(@PathVariable("id") Long id) {
        workOrderService.delete(id, UserContext.require());
        return R.ok();
    }
}
