package com.city.platform.event.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.city.platform.event.entity.Notification;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

@Mapper
public interface NotificationMapper extends BaseMapper<Notification> {

    /**
     * 唯一索引保证同一用户同一业务只落一条通知，重复投递安全。
     */
    @Insert("INSERT IGNORE INTO t_notification (user_id, title, content, biz_type, biz_id, is_read) "
            + "VALUES (#{userId}, #{title}, #{content}, #{bizType}, #{bizId}, 0)")
    int insertIgnore(@Param("userId") Long userId,
                     @Param("title") String title,
                     @Param("content") String content,
                     @Param("bizType") String bizType,
                     @Param("bizId") Long bizId);

    /**
     * 带上发送人的通知写入：同一业务重复提醒时刷新为未读，避免静默失败。
     */
    @Insert("INSERT INTO t_notification (user_id, sender_id, title, content, biz_type, biz_id, is_read) "
            + "VALUES (#{userId}, #{senderId}, #{title}, #{content}, #{bizType}, #{bizId}, 0) "
            + "ON DUPLICATE KEY UPDATE sender_id = VALUES(sender_id), title = VALUES(title), "
            + "content = VALUES(content), is_read = 0, create_time = NOW()")
    int upsert(@Param("userId") Long userId,
               @Param("senderId") Long senderId,
               @Param("title") String title,
               @Param("content") String content,
               @Param("bizType") String bizType,
               @Param("bizId") Long bizId);
}
