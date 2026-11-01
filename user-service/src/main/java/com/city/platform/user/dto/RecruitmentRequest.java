package com.city.platform.user.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.time.LocalDate;

@Data
public class RecruitmentRequest {

    @NotBlank(message = "岗位标题不能为空")
    @Size(max = 128, message = "标题长度不能超过 128")
    private String title;

    @NotBlank(message = "部门不能为空")
    private String deptCode;

    @Size(max = 64, message = "片区名称长度不能超过 64")
    private String workZone;

    @Min(value = 1, message = "招募人数至少为 1")
    private Integer headcount;

    @Size(max = 500, message = "岗位要求长度不能超过 500")
    private String requirement;

    @Size(max = 1000, message = "岗位说明长度不能超过 1000")
    private String description;

    private LocalDate deadline;

    @Size(max = 512, message = "封面图地址长度不能超过 512")
    private String coverUrl;
}
