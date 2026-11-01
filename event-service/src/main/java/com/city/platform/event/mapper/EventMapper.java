package com.city.platform.event.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.city.platform.event.dto.AreaCount;
import com.city.platform.event.dto.CategoryCount;
import com.city.platform.event.dto.DateCount;
import com.city.platform.event.dto.StatusCount;
import com.city.platform.event.entity.Event;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

import java.time.LocalDateTime;
import java.util.List;

@Mapper
public interface EventMapper extends BaseMapper<Event> {

    @Select("SELECT status AS status, COUNT(*) AS cnt FROM t_event WHERE deleted = 0 GROUP BY status")
    List<StatusCount> countByStatus();

    /**
     * 状态条件更新：受影响行数为 0 即视为状态冲突，用于防止并发非法跳转。
     */
    @Update("UPDATE t_event SET status = #{toStatus}, "
            + "close_time = CASE WHEN #{toStatus} = 'CLOSED' THEN NOW() ELSE close_time END, "
            + "update_time = NOW(), version = version + 1 "
            + "WHERE id = #{id} AND status = #{fromStatus} AND deleted = 0")
    int updateStatus(@Param("id") Long id,
                     @Param("fromStatus") String fromStatus,
                     @Param("toStatus") String toStatus);

    @Update("UPDATE t_event SET status = 'DISPATCHED', category_code = #{categoryCode}, "
            + "priority = #{priority}, area_code = #{areaCode}, auditor_id = #{auditorId}, "
            + "audit_time = NOW(), dispatch_time = NOW(), update_time = NOW(), version = version + 1 "
            + "WHERE id = #{id} AND status = 'PENDING_REVIEW' AND deleted = 0")
    int approveWithFields(@Param("id") Long id,
                          @Param("categoryCode") String categoryCode,
                          @Param("priority") String priority,
                          @Param("areaCode") String areaCode,
                          @Param("auditorId") Long auditorId);

    @Update("UPDATE t_event SET status = 'REJECTED', auditor_id = #{auditorId}, audit_time = NOW(), "
            + "reject_reason = #{reason}, update_time = NOW(), version = version + 1 "
            + "WHERE id = #{id} AND status = 'PENDING_REVIEW' AND deleted = 0")
    int reject(@Param("id") Long id, @Param("auditorId") Long auditorId, @Param("reason") String reason);

    /**
     * 自动分流派单：系统按片区匹配组长，不记录人工审核人。
     */
    @Update("UPDATE t_event SET status = 'DISPATCHED', category_code = #{categoryCode}, "
            + "priority = #{priority}, area_code = #{areaCode}, auditor_id = NULL, "
            + "dispatch_time = NOW(), update_time = NOW(), version = version + 1 "
            + "WHERE id = #{id} AND status = 'PENDING_REVIEW' AND deleted = 0")
    int autoDispatch(@Param("id") Long id,
                     @Param("categoryCode") String categoryCode,
                     @Param("priority") String priority,
                     @Param("areaCode") String areaCode);

    @Select("<script>SELECT category_code AS code, COUNT(*) AS cnt FROM t_event WHERE deleted = 0"
            + "<if test='startTime != null'> AND create_time &gt;= #{startTime}</if>"
            + "<if test='endTime != null'> AND create_time &lt;= #{endTime}</if>"
            + " GROUP BY category_code ORDER BY cnt DESC</script>")
    List<CategoryCount> countByCategory(@Param("startTime") LocalDateTime startTime,
                                        @Param("endTime") LocalDateTime endTime);

    @Select("<script>SELECT area_code AS areaCode, "
            + "COALESCE(MAX(NULLIF(district, '')), area_code) AS areaName, COUNT(*) AS cnt "
            + "FROM t_event WHERE deleted = 0"
            + "<if test='startTime != null'> AND create_time &gt;= #{startTime}</if>"
            + "<if test='endTime != null'> AND create_time &lt;= #{endTime}</if>"
            + " GROUP BY area_code ORDER BY cnt DESC LIMIT 20</script>")
    List<AreaCount> countByArea(@Param("startTime") LocalDateTime startTime,
                                @Param("endTime") LocalDateTime endTime);

    @Select("SELECT DATE_FORMAT(create_time, '%Y-%m-%d') AS date, COUNT(*) AS cnt "
            + "FROM t_event WHERE deleted = 0 AND create_time >= #{startTime} AND create_time < #{endTime} "
            + "GROUP BY date ORDER BY date")
    List<DateCount> countByDate(@Param("startTime") LocalDateTime startTime,
                                @Param("endTime") LocalDateTime endTime);
}
