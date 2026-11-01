package com.city.platform.common.api.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

/**
 * 处理人员档案快照。
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class WorkerProfile implements Serializable {

    private Long userId;
    private String nickname;
    private String deptCode;
    private String deptName;
    private String status;
    /** LEADER / MEMBER */
    private String workerType;
    /** 组员所属组长 */
    private Long leaderId;
    /** 负责片区行政区划编码 */
    private String areaCode;
    /** 负责片区名称 */
    private String workZone;
}
