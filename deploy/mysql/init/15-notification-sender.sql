-- 通知增加发送人：管理员提醒组长、组长转达组员时记录来源
ALTER TABLE `city_event`.`t_notification`
    ADD COLUMN `sender_id` BIGINT UNSIGNED NULL COMMENT '发送人用户ID，系统通知为空' AFTER `user_id`;
