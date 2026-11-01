package com.city.platform.user.service;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.city.platform.common.core.exception.BizException;
import com.city.platform.common.core.exception.ErrorCode;
import com.city.platform.common.security.constant.RoleConstants;
import com.city.platform.common.security.context.UserContext;
import com.city.platform.user.dto.RecruitmentRequest;
import com.city.platform.user.entity.Recruitment;
import com.city.platform.user.entity.RecruitmentApply;
import com.city.platform.user.entity.User;
import com.city.platform.user.mapper.RecruitmentApplyMapper;
import com.city.platform.user.mapper.RecruitmentMapper;
import com.city.platform.user.mapper.UserMapper;
import com.city.platform.user.vo.RecruitmentVO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.time.LocalDate;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

@Slf4j
@Service
@RequiredArgsConstructor
public class RecruitmentService {

    private static final Map<String, String> DEPT_NAMES = Map.of(
            "ROAD", "市政道路班组",
            "LIGHTING", "路灯照明班组",
            "SANITATION", "环卫保洁班组",
            "FACILITY", "公共设施班组");
    private static final Set<String> APPLY_STATUS = Set.of("PENDING", "ACCEPTED", "REJECTED");

    private final RecruitmentMapper recruitmentMapper;
    private final RecruitmentApplyMapper applyMapper;
    private final UserMapper userMapper;

    public List<RecruitmentVO> adminList() {
        List<Recruitment> items = recruitmentMapper.selectList(Wrappers.<Recruitment>lambdaQuery()
                .orderByDesc(Recruitment::getCreateTime));
        return items.stream().map(item -> {
            RecruitmentVO vo = RecruitmentVO.of(item);
            vo.setApplyCount(applyMapper.selectCount(Wrappers.<RecruitmentApply>lambdaQuery()
                    .eq(RecruitmentApply::getRecruitmentId, item.getId())));
            return vo;
        }).toList();
    }

    public RecruitmentVO create(RecruitmentRequest request) {
        String deptCode = request.getDeptCode().trim();
        String deptName = DEPT_NAMES.get(deptCode);
        if (deptName == null) {
            throw new BizException(ErrorCode.PARAM_INVALID, "部门编码只允许 ROAD/LIGHTING/SANITATION/FACILITY");
        }
        Recruitment item = new Recruitment();
        item.setTitle(request.getTitle().trim());
        item.setDeptCode(deptCode);
        item.setDeptName(deptName);
        item.setWorkZone(request.getWorkZone());
        item.setHeadcount(request.getHeadcount() == null ? 1 : request.getHeadcount());
        item.setRequirement(request.getRequirement());
        item.setDescription(request.getDescription());
        item.setDeadline(request.getDeadline());
        item.setCoverUrl(request.getCoverUrl());
        item.setStatus("OPEN");
        recruitmentMapper.insert(item);
        log.info("发布招募公告 id={} title={} operator={}", item.getId(), item.getTitle(), currentUserId());
        return RecruitmentVO.of(item);
    }

    public RecruitmentVO update(Long id, RecruitmentRequest request) {
        Recruitment item = requireRecruitment(id);
        String deptCode = request.getDeptCode().trim();
        String deptName = DEPT_NAMES.get(deptCode);
        if (deptName == null) {
            throw new BizException(ErrorCode.PARAM_INVALID, "部门编码只允许 ROAD/LIGHTING/SANITATION/FACILITY");
        }
        Recruitment update = new Recruitment();
        update.setId(item.getId());
        update.setTitle(request.getTitle().trim());
        update.setDeptCode(deptCode);
        update.setDeptName(deptName);
        update.setWorkZone(request.getWorkZone());
        update.setHeadcount(request.getHeadcount() == null ? 1 : request.getHeadcount());
        update.setRequirement(request.getRequirement());
        update.setDescription(request.getDescription());
        update.setDeadline(request.getDeadline());
        update.setCoverUrl(request.getCoverUrl());
        recruitmentMapper.updateById(update);
        return RecruitmentVO.of(recruitmentMapper.selectById(id));
    }

    public RecruitmentVO updateStatus(Long id, String status) {
        if (!"OPEN".equals(status) && !"CLOSED".equals(status)) {
            throw new BizException(ErrorCode.PARAM_INVALID, "状态只允许 OPEN 或 CLOSED");
        }
        Recruitment item = requireRecruitment(id);
        Recruitment update = new Recruitment();
        update.setId(item.getId());
        update.setStatus(status);
        recruitmentMapper.updateById(update);
        return RecruitmentVO.of(recruitmentMapper.selectById(id));
    }

    /**
     * 居民端：只返回招募中且未过期的岗位。
     */
    public List<RecruitmentVO> openList() {
        Long userId = currentUserId();
        List<Recruitment> items = recruitmentMapper.selectList(Wrappers.<Recruitment>lambdaQuery()
                .eq(Recruitment::getStatus, "OPEN")
                .and(wrapper -> wrapper.isNull(Recruitment::getDeadline)
                        .or().ge(Recruitment::getDeadline, LocalDate.now()))
                .orderByDesc(Recruitment::getCreateTime));
        List<Long> appliedIds = appliedRecruitmentIds(userId);
        return items.stream().map(item -> {
            RecruitmentVO vo = RecruitmentVO.of(item);
            vo.setApplyCount(applyMapper.selectCount(Wrappers.<RecruitmentApply>lambdaQuery()
                    .eq(RecruitmentApply::getRecruitmentId, item.getId())));
            vo.setAppliedByMe(appliedIds.contains(item.getId()));
            return vo;
        }).toList();
    }

    public RecruitmentVO detail(Long id) {
        Recruitment item = requireRecruitment(id);
        Long userId = currentUserId();
        RecruitmentVO vo = RecruitmentVO.of(item);
        vo.setApplyCount(applyMapper.selectCount(Wrappers.<RecruitmentApply>lambdaQuery()
                .eq(RecruitmentApply::getRecruitmentId, id)));
        vo.setAppliedByMe(appliedRecruitmentIds(userId).contains(id));
        return vo;
    }

    public Map<String, Object> apply(Long id, String note) {
        Long userId = currentUserId();
        if (!userMapper.selectRoleCodes(userId).contains(RoleConstants.RESIDENT)) {
            throw new BizException(ErrorCode.FORBIDDEN, "仅居民可报名");
        }
        User applicant = userMapper.selectById(userId);
        if (applicant == null || !StringUtils.hasText(applicant.getPhone())) {
            throw new BizException(ErrorCode.STATE_CONFLICT, "请先在“我的”页绑定手机号后再报名，方便管理员联系你");
        }
        Recruitment item = requireRecruitment(id);
        if (!"OPEN".equals(item.getStatus())) {
            throw new BizException(ErrorCode.STATE_CONFLICT, "该岗位已停止招募");
        }
        if (item.getDeadline() != null && item.getDeadline().isBefore(LocalDate.now())) {
            throw new BizException(ErrorCode.STATE_CONFLICT, "该岗位已过报名截止时间");
        }
        RecruitmentApply apply = new RecruitmentApply();
        apply.setRecruitmentId(id);
        apply.setUserId(userId);
        apply.setNote(StringUtils.hasText(note) ? note.trim() : null);
        apply.setStatus("PENDING");
        try {
            applyMapper.insert(apply);
        } catch (DuplicateKeyException e) {
            throw new BizException(ErrorCode.STATE_CONFLICT, "已报名该岗位，请勿重复提交");
        }
        log.info("居民报名招募 recruitmentId={} userId={}", id, userId);
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("applyId", apply.getId());
        result.put("status", "PENDING");
        return result;
    }

    public List<Map<String, Object>> myApplications() {
        Long userId = currentUserId();
        return applyMapper.selectList(Wrappers.<RecruitmentApply>lambdaQuery()
                        .eq(RecruitmentApply::getUserId, userId)
                        .orderByDesc(RecruitmentApply::getCreateTime))
                .stream().map(apply -> {
                    Recruitment item = recruitmentMapper.selectById(apply.getRecruitmentId());
                    Map<String, Object> row = new LinkedHashMap<>();
                    row.put("applyId", apply.getId());
                    row.put("recruitmentId", apply.getRecruitmentId());
                    row.put("title", item == null ? "岗位已下架" : item.getTitle());
                    row.put("applyStatus", apply.getStatus());
                    row.put("createTime", apply.getCreateTime());
                    return row;
                }).toList();
    }

    public List<Map<String, Object>> applications(Long id) {
        requireRecruitment(id);
        return applyMapper.selectList(Wrappers.<RecruitmentApply>lambdaQuery()
                        .eq(RecruitmentApply::getRecruitmentId, id)
                        .orderByDesc(RecruitmentApply::getCreateTime))
                .stream().map(apply -> {
                    User user = userMapper.selectById(apply.getUserId());
                    Map<String, Object> row = new LinkedHashMap<>();
                    row.put("applyId", apply.getId());
                    row.put("userId", apply.getUserId());
                    row.put("nickname", user == null ? "-" : user.getNickname());
                    row.put("username", user == null ? "-" : user.getUsername());
                    row.put("phone", user == null || !StringUtils.hasText(user.getPhone()) ? "-" : user.getPhone());
                    row.put("email", user == null || !StringUtils.hasText(user.getEmail()) ? "-" : user.getEmail());
                    row.put("note", apply.getNote());
                    row.put("status", apply.getStatus());
                    row.put("createTime", apply.getCreateTime());
                    return row;
                }).toList();
    }

    public void updateApplyStatus(Long applyId, String status) {
        if (!APPLY_STATUS.contains(status)) {
            throw new BizException(ErrorCode.PARAM_INVALID, "报名状态只允许 PENDING/ACCEPTED/REJECTED");
        }
        RecruitmentApply apply = applyMapper.selectById(applyId);
        if (apply == null) {
            throw new BizException(ErrorCode.NOT_FOUND, "报名记录不存在");
        }
        RecruitmentApply update = new RecruitmentApply();
        update.setId(applyId);
        update.setStatus(status);
        applyMapper.updateById(update);
    }

    private List<Long> appliedRecruitmentIds(Long userId) {
        return applyMapper.selectList(Wrappers.<RecruitmentApply>lambdaQuery()
                        .eq(RecruitmentApply::getUserId, userId))
                .stream().map(RecruitmentApply::getRecruitmentId).toList();
    }

    private Recruitment requireRecruitment(Long id) {
        Recruitment item = recruitmentMapper.selectById(id);
        if (item == null) {
            throw new BizException(ErrorCode.NOT_FOUND, "招募岗位不存在");
        }
        return item;
    }

    private Long currentUserId() {
        return UserContext.require().getUserId();
    }
}
