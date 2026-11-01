package com.city.platform.workorder.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.city.platform.workorder.entity.WorkOrder;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Update;

@Mapper
public interface WorkOrderMapper extends BaseMapper<WorkOrder> {

    @Update("UPDATE t_work_order SET status = 'PROCESSING', accept_time = NOW(), "
            + "update_time = NOW(), version = version + 1 "
            + "WHERE id = #{id} AND assignee_id = #{assigneeId} AND status = 'PENDING_ACCEPT' AND deleted = 0")
    int accept(@Param("id") Long id, @Param("assigneeId") Long assigneeId);

    @Update("UPDATE t_work_order SET status = 'PENDING_ACCEPT', accept_time = NULL, "
            + "update_time = NOW(), version = version + 1 "
            + "WHERE id = #{id} AND assignee_id = #{assigneeId} AND status = 'PROCESSING' AND deleted = 0")
    int cancelAccept(@Param("id") Long id, @Param("assigneeId") Long assigneeId);

    @Update("UPDATE t_work_order SET status = 'PROCESSING', submit_time = NULL, "
            + "update_time = NOW(), version = version + 1 "
            + "WHERE id = #{id} AND assignee_id = #{assigneeId} "
            + "AND status IN ('PENDING_LEADER_REVIEW', 'PENDING_ACCEPTANCE') AND deleted = 0")
    int withdrawSubmit(@Param("id") Long id, @Param("assigneeId") Long assigneeId);

    @Update("UPDATE t_work_order SET status = #{targetStatus}, submit_time = NOW(), "
            + "update_time = NOW(), version = version + 1 "
            + "WHERE id = #{id} AND assignee_id = #{assigneeId} AND status = 'PROCESSING' AND deleted = 0")
    int submit(@Param("id") Long id, @Param("assigneeId") Long assigneeId,
               @Param("targetStatus") String targetStatus);

    @Update("UPDATE t_work_order SET status = 'PENDING_ACCEPTANCE', leader_review_time = NOW(), "
            + "leader_review_remark = #{remark}, update_time = NOW(), version = version + 1 "
            + "WHERE id = #{id} AND leader_id = #{leaderId} AND status = 'PENDING_LEADER_REVIEW' AND deleted = 0")
    int leaderPass(@Param("id") Long id, @Param("leaderId") Long leaderId,
                   @Param("remark") String remark);

    @Update("UPDATE t_work_order SET status = 'PROCESSING', return_count = return_count + 1, "
            + "leader_review_time = NOW(), leader_review_remark = #{remark}, "
            + "update_time = NOW(), version = version + 1 "
            + "WHERE id = #{id} AND leader_id = #{leaderId} AND status = 'PENDING_LEADER_REVIEW' AND deleted = 0")
    int leaderReject(@Param("id") Long id, @Param("leaderId") Long leaderId,
                     @Param("remark") String remark);

    @Update("UPDATE t_work_order SET assignee_id = #{assigneeId}, assignee_name = #{assigneeName}, "
            + "dept_code = #{deptCode}, status = 'PENDING_ACCEPT', assign_time = NOW(), "
            + "reassign_count = reassign_count + 1, update_time = NOW(), version = version + 1 "
            + "WHERE id = #{id} AND leader_id = #{leaderId} "
            + "AND status IN ('PENDING_ACCEPT', 'PROCESSING') AND deleted = 0")
    int assign(@Param("id") Long id, @Param("leaderId") Long leaderId,
               @Param("assigneeId") Long assigneeId,
               @Param("assigneeName") String assigneeName,
               @Param("deptCode") String deptCode);

    @Update("UPDATE t_work_order SET status = 'COMPLETED', finish_time = NOW(), "
            + "acceptance_remark = #{remark}, update_time = NOW(), version = version + 1 "
            + "WHERE id = #{id} AND status = 'PENDING_ACCEPTANCE' AND deleted = 0")
    int acceptPass(@Param("id") Long id, @Param("remark") String remark);

    @Update("UPDATE t_work_order SET status = 'PROCESSING', return_count = return_count + 1, "
            + "acceptance_remark = #{remark}, update_time = NOW(), version = version + 1 "
            + "WHERE id = #{id} AND status = 'PENDING_ACCEPTANCE' AND deleted = 0")
    int acceptReject(@Param("id") Long id, @Param("remark") String remark);

    @Update("UPDATE t_work_order SET assignee_id = #{assigneeId}, assignee_name = #{assigneeName}, "
            + "dept_code = #{deptCode}, status = 'PENDING_ACCEPT', assign_time = NOW(), "
            + "reassign_count = reassign_count + 1, update_time = NOW(), version = version + 1 "
            + "WHERE id = #{id} AND status IN ('PENDING_ACCEPT', 'PROCESSING') AND deleted = 0")
    int reassign(@Param("id") Long id,
                 @Param("assigneeId") Long assigneeId,
                 @Param("assigneeName") String assigneeName,
                 @Param("deptCode") String deptCode);
}
