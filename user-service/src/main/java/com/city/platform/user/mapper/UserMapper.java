package com.city.platform.user.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.city.platform.user.entity.User;
import com.city.platform.user.vo.WorkerVO;
import lombok.Data;
import org.apache.ibatis.annotations.Delete;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

import java.util.List;

public interface UserMapper extends BaseMapper<User> {

    @Select("SELECT role_code FROM t_user_role WHERE user_id = #{userId} ORDER BY id")
    List<String> selectRoleCodes(@Param("userId") Long userId);

    @Select("<script>SELECT user_id AS userId, role_code AS roleCode FROM t_user_role "
            + "WHERE user_id IN <foreach collection='userIds' item='id' open='(' separator=',' close=')'>#{id}</foreach> "
            + "ORDER BY id</script>")
    List<UserRoleItem> selectRoleItems(@Param("userIds") List<Long> userIds);

    @Insert("INSERT INTO t_user_role (user_id, role_code) VALUES (#{userId}, #{roleCode})")
    int insertUserRole(@Param("userId") Long userId, @Param("roleCode") String roleCode);

    @Delete("DELETE FROM t_user_role WHERE user_id = #{userId}")
    int deleteUserRoles(@Param("userId") Long userId);

    /**
     * 解绑微信：显式置空，避免 MyBatis-Plus 默认忽略 null 字段。
     */
    @Update("UPDATE t_user SET open_id = NULL, union_id = NULL WHERE id = #{userId}")
    int clearWechatBinding(@Param("userId") Long userId);

    /**
     * 解绑邮箱：显式置空，避免 MyBatis-Plus 默认忽略 null 字段。
     */
    @Update("UPDATE t_user SET email = NULL, email_verified = 0 WHERE id = #{userId}")
    int clearEmail(@Param("userId") Long userId);

    @Select("SELECT u.id, u.username, u.phone, u.nickname, w.dept_code AS deptCode, w.dept_name AS deptName, "
            + "w.area_code AS areaCode, w.status, w.worker_type AS workerType, w.leader_id AS leaderId, "
            + "w.work_zone AS workZone, w.zone_lng AS zoneLng, w.zone_lat AS zoneLat, lu.nickname AS leaderName "
            + "FROM t_user u "
            + "JOIN t_user_role r ON r.user_id = u.id AND r.role_code = 'WORKER' "
            + "JOIN t_worker_profile w ON w.user_id = u.id "
            + "LEFT JOIN t_user lu ON lu.id = w.leader_id "
            + "WHERE u.deleted = 0 AND w.deleted = 0 ORDER BY w.worker_type DESC, u.id")
    List<WorkerVO> selectWorkers();

    @Select("SELECT u.id, u.username, u.phone, u.nickname, w.dept_code AS deptCode, w.dept_name AS deptName, "
            + "w.area_code AS areaCode, w.status, w.worker_type AS workerType, w.leader_id AS leaderId, "
            + "w.work_zone AS workZone, w.zone_lng AS zoneLng, w.zone_lat AS zoneLat, lu.nickname AS leaderName "
            + "FROM t_user u "
            + "JOIN t_user_role r ON r.user_id = u.id AND r.role_code = 'WORKER' "
            + "JOIN t_worker_profile w ON w.user_id = u.id "
            + "LEFT JOIN t_user lu ON lu.id = w.leader_id "
            + "WHERE u.deleted = 0 AND w.deleted = 0 AND u.id = #{userId}")
    WorkerVO selectWorker(@Param("userId") Long userId);

    @Data
    class UserRoleItem {
        private Long userId;
        private String roleCode;
    }
}
