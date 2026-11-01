SET NAMES utf8mb4;

-- P5：工单分配通知（消费者幂等落库）
USE city_event;

CREATE TABLE IF NOT EXISTS `t_notification`
(
    `id`          BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
    `user_id`     BIGINT UNSIGNED NOT NULL COMMENT '接收人',
    `title`       VARCHAR(128)    NOT NULL,
    `content`     VARCHAR(512)    NOT NULL,
    `biz_type`    VARCHAR(32)     NOT NULL COMMENT 'WORK_ORDER_ASSIGNED',
    `biz_id`      BIGINT UNSIGNED NOT NULL COMMENT '工单ID',
    `is_read`     TINYINT         NOT NULL DEFAULT 0,
    `create_time` DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_notify_user_biz` (`user_id`, `biz_type`, `biz_id`),
    KEY `idx_notify_user_time` (`user_id`, `create_time`)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT ='站内通知';
