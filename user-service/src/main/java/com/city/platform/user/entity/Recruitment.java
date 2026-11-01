package com.city.platform.user.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.city.platform.common.core.model.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.LocalDate;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("t_recruitment")
public class Recruitment extends BaseEntity {

    @TableId(type = IdType.AUTO)
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
}
