package com.city.platform.user.vo;

import com.city.platform.user.entity.User;
import com.city.platform.user.entity.WorkerProfileEntity;
import lombok.Data;

import java.math.BigDecimal;

@Data
public class WorkerVO {

    private Long id;
    private String username;
    private String nickname;
    private String phone;
    private String deptCode;
    private String deptName;
    private String areaCode;
    private String status;
    /** LEADER 组长 / MEMBER 组员 */
    private String workerType;
    private Long leaderId;
    private String leaderName;
    private String workZone;
    private BigDecimal zoneLng;
    private BigDecimal zoneLat;
    /** 当前在岗工单数：工单在独立库，由 workorder-service 提供，此处暂固定为 0 */
    private int onDutyCount;

    public static WorkerVO of(User user, WorkerProfileEntity profile) {
        WorkerVO vo = new WorkerVO();
        vo.setId(user.getId());
        vo.setUsername(user.getUsername());
        vo.setNickname(user.getNickname());
        vo.setPhone(UserVO.maskPhone(user.getPhone()));
        if (profile != null) {
            vo.setDeptCode(profile.getDeptCode());
            vo.setDeptName(profile.getDeptName());
            vo.setAreaCode(profile.getAreaCode());
            vo.setStatus(profile.getStatus());
            vo.setWorkerType(profile.getWorkerType());
            vo.setLeaderId(profile.getLeaderId());
            vo.setWorkZone(profile.getWorkZone());
            vo.setZoneLng(profile.getZoneLng());
            vo.setZoneLat(profile.getZoneLat());
        }
        return vo;
    }
}
