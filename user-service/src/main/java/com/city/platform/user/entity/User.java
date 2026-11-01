package com.city.platform.user.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.city.platform.common.core.model.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.LocalDateTime;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("t_user")
public class User extends BaseEntity {

    @TableId(type = IdType.AUTO)
    private Long id;

    private String openId;

    private String unionId;

    private String username;

    private String passwordHash;

    private String nickname;

    private String avatarUrl;

    private String phone;

    private String email;

    private Integer emailVerified;

    private String status;

    private LocalDateTime lastLoginTime;
}
