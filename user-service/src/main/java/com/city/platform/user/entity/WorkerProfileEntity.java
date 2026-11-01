package com.city.platform.user.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.city.platform.common.core.model.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.math.BigDecimal;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("t_worker_profile")
public class WorkerProfileEntity extends BaseEntity {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long userId;

    /** LEADER 组长 / MEMBER 组员 */
    private String workerType;

    /** 所属组长用户ID（组员必填） */
    private Long leaderId;

    private String deptCode;

    private String deptName;

    private String areaCode;

    private String workZone;

    private BigDecimal zoneLng;

    private BigDecimal zoneLat;

    private String skillTags;

    private String status;
}
