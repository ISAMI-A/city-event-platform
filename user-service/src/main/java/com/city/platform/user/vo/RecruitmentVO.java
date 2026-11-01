package com.city.platform.user.vo;

import com.city.platform.user.entity.Recruitment;
import lombok.Data;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
public class RecruitmentVO {

    private Long id;
    private String title;
    private String deptCode;
    private String deptName;
    private String workZone;
    private Integer headcount;
    private String requirement;
    private String description;
    private String status;
    private LocalDate deadline;
    private String coverUrl;
    private LocalDateTime createTime;
    private long applyCount;
    private boolean appliedByMe;

    public static RecruitmentVO of(Recruitment item) {
        RecruitmentVO vo = new RecruitmentVO();
        vo.setId(item.getId());
        vo.setTitle(item.getTitle());
        vo.setDeptCode(item.getDeptCode());
        vo.setDeptName(item.getDeptName());
        vo.setWorkZone(item.getWorkZone());
        vo.setHeadcount(item.getHeadcount());
        vo.setRequirement(item.getRequirement());
        vo.setDescription(item.getDescription());
        vo.setStatus(item.getStatus());
        vo.setDeadline(item.getDeadline());
        vo.setCoverUrl(item.getCoverUrl());
        vo.setCreateTime(item.getCreateTime());
        return vo;
    }
}
