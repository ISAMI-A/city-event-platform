package com.city.platform.user.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.city.platform.common.core.model.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("t_recruitment_apply")
public class RecruitmentApply extends BaseEntity {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long recruitmentId;
    private Long userId;
    private String note;
    private String status;
}
