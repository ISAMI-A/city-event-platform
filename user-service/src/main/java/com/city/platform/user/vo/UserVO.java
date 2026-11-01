package com.city.platform.user.vo;

import com.city.platform.user.entity.User;
import com.city.platform.user.entity.WorkerProfileEntity;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

@Data
public class UserVO {

    private Long id;
    private String username;
    private String nickname;
    private String avatarUrl;
    private String phone;
    private String status;
    private LocalDateTime createTime;
    private LocalDateTime lastLoginTime;
    private List<String> roles;
    /** 仅详情接口在存在工人档案时返回 */
    private WorkerProfileVO workerProfile;

    public static UserVO from(User user) {
        UserVO vo = new UserVO();
        vo.setId(user.getId());
        vo.setUsername(user.getUsername());
        vo.setNickname(user.getNickname());
        vo.setAvatarUrl(user.getAvatarUrl());
        vo.setPhone(maskPhone(user.getPhone()));
        vo.setStatus(user.getStatus());
        vo.setCreateTime(user.getCreateTime());
        vo.setLastLoginTime(user.getLastLoginTime());
        return vo;
    }

    public static String maskPhone(String phone) {
        if (phone == null || phone.length() < 7) {
            return phone;
        }
        return phone.substring(0, 3) + "****" + phone.substring(phone.length() - 4);
    }

    @Data
    public static class WorkerProfileVO {

        private String deptCode;
        private String deptName;
        private String areaCode;
        private String status;

        public static WorkerProfileVO from(WorkerProfileEntity profile) {
            WorkerProfileVO vo = new WorkerProfileVO();
            vo.setDeptCode(profile.getDeptCode());
            vo.setDeptName(profile.getDeptName());
            vo.setAreaCode(profile.getAreaCode());
            vo.setStatus(profile.getStatus());
            return vo;
        }
    }
}
