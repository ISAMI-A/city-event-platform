SET NAMES utf8mb4;

USE city_user;

CREATE TABLE IF NOT EXISTS `t_user`
(
    `id`              BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '用户ID',
    `open_id`         VARCHAR(64)     NULL COMMENT '微信openid',
    `union_id`        VARCHAR(64)     NULL COMMENT '微信unionid',
    `username`        VARCHAR(64)     NULL COMMENT '登录账号',
    `password_hash`   VARCHAR(100)    NULL COMMENT 'BCrypt密码哈希',
    `nickname`        VARCHAR(64)     NOT NULL COMMENT '昵称',
    `avatar_url`      VARCHAR(512)    NULL COMMENT '头像',
    `phone`           VARCHAR(20)     NULL COMMENT '手机号',
    `status`          VARCHAR(16)     NOT NULL DEFAULT 'ENABLED' COMMENT 'ENABLED/DISABLED',
    `last_login_time` DATETIME        NULL,
    `create_time`     DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP,
    `update_time`     DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    `create_by`       BIGINT UNSIGNED NULL,
    `update_by`       BIGINT UNSIGNED NULL,
    `deleted`         TINYINT         NOT NULL DEFAULT 0,
    `version`         INT             NOT NULL DEFAULT 0,
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_user_open_id` (`open_id`),
    UNIQUE KEY `uk_user_username` (`username`),
    KEY `idx_user_status` (`status`)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT ='用户';

CREATE TABLE IF NOT EXISTS `t_role`
(
    `id`          BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
    `role_code`   VARCHAR(32)     NOT NULL,
    `role_name`   VARCHAR(64)     NOT NULL,
    `description` VARCHAR(255)    NULL,
    `create_time` DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP,
    `update_time` DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    `deleted`     TINYINT         NOT NULL DEFAULT 0,
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_role_code` (`role_code`)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT ='角色';

CREATE TABLE IF NOT EXISTS `t_user_role`
(
    `id`          BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
    `user_id`     BIGINT UNSIGNED NOT NULL,
    `role_code`   VARCHAR(32)     NOT NULL,
    `create_time` DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_user_role` (`user_id`, `role_code`),
    KEY `idx_user_role_code` (`role_code`)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT ='用户角色关联';

CREATE TABLE IF NOT EXISTS `t_worker_profile`
(
    `id`          BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
    `user_id`     BIGINT UNSIGNED NOT NULL,
    `dept_code`   VARCHAR(32)     NOT NULL COMMENT 'ROAD/LIGHTING/SANITATION/FACILITY',
    `dept_name`   VARCHAR(64)     NOT NULL,
    `area_code`   VARCHAR(32)     NULL,
    `skill_tags`  VARCHAR(255)    NULL,
    `status`      VARCHAR(16)     NOT NULL DEFAULT 'ON_DUTY',
    `create_time` DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP,
    `update_time` DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    `create_by`   BIGINT UNSIGNED NULL,
    `update_by`   BIGINT UNSIGNED NULL,
    `deleted`     TINYINT         NOT NULL DEFAULT 0,
    `version`     INT             NOT NULL DEFAULT 0,
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_worker_user_id` (`user_id`),
    KEY `idx_worker_dept` (`dept_code`),
    KEY `idx_worker_area` (`area_code`)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT ='处理人员档案';

-- 角色字典
INSERT INTO `t_role` (`role_code`, `role_name`, `description`)
VALUES ('RESIDENT', '居民', '上报事件与查看进度'),
       ('WORKER', '处理人员', '接单与提交处理结果'),
       ('ADMIN', '管理员', '审核、派单、验收与用户管理')
ON DUPLICATE KEY UPDATE `role_name` = VALUES(`role_name`), `description` = VALUES(`description`);

-- 内置账号（仅开发/演示环境；生产必须首次登录改密）
-- admin / Admin@123
INSERT IGNORE INTO `t_user` (`id`, `username`, `password_hash`, `nickname`, `status`)
VALUES (1, 'admin', '$2a$10$t0MnAx9OHYy58FUJjOiwiePMTSqMr1xbk9W7rjkagunHVUZi1rlHi', '系统管理员', 'ENABLED');

-- worker_road / worker_lighting / worker_sanitation，密码均为 Worker@123
INSERT IGNORE INTO `t_user` (`id`, `username`, `password_hash`, `nickname`, `status`)
VALUES (2, 'worker_road', '$2a$10$5kOPPM5Z9jgccQoEkHlkoOLTzbT8nGorVXcKsoXA1dN0NBsa/Rxb.', '李道路', 'ENABLED'),
       (3, 'worker_lighting', '$2a$10$5kOPPM5Z9jgccQoEkHlkoOLTzbT8nGorVXcKsoXA1dN0NBsa/Rxb.', '王照明', 'ENABLED'),
       (4, 'worker_sanitation', '$2a$10$5kOPPM5Z9jgccQoEkHlkoOLTzbT8nGorVXcKsoXA1dN0NBsa/Rxb.', '张环卫', 'ENABLED');

INSERT IGNORE INTO `t_user_role` (`user_id`, `role_code`)
VALUES (1, 'ADMIN'),
       (2, 'WORKER'),
       (3, 'WORKER'),
       (4, 'WORKER');

INSERT IGNORE INTO `t_worker_profile` (`user_id`, `dept_code`, `dept_name`, `area_code`, `status`)
VALUES (2, 'ROAD', '市政道路班组', '110101', 'ON_DUTY'),
       (3, 'LIGHTING', '路灯照明班组', '110101', 'ON_DUTY'),
       (4, 'SANITATION', '环卫保洁班组', '110101', 'ON_DUTY');
