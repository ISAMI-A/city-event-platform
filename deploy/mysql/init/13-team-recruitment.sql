SET NAMES utf8mb4;

-- ============================================================
-- P6：组长/组员两级组织、两级验收、居民评分、招募公告、AI 紧急度复评
-- 仅需执行一次（新环境由 init 顺序自动执行）
-- ============================================================

USE city_user;
ALTER TABLE `t_worker_profile`
    ADD COLUMN `worker_type` VARCHAR(16) NOT NULL DEFAULT 'MEMBER' COMMENT 'LEADER/MEMBER' AFTER `user_id`,
    ADD COLUMN `leader_id` BIGINT UNSIGNED NULL COMMENT '所属组长用户ID' AFTER `worker_type`,
    ADD COLUMN `work_zone` VARCHAR(64) NULL COMMENT '负责片区名称' AFTER `area_code`,
    ADD COLUMN `zone_lng` DECIMAL(10, 6) NULL COMMENT '片区中心经度' AFTER `work_zone`,
    ADD COLUMN `zone_lat` DECIMAL(10, 6) NULL COMMENT '片区中心纬度' AFTER `zone_lng`,
    ADD KEY `idx_worker_leader` (`leader_id`),
    ADD KEY `idx_worker_type` (`worker_type`);

CREATE TABLE IF NOT EXISTS `t_recruitment`
(
    `id`          BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
    `title`       VARCHAR(128)    NOT NULL COMMENT '岗位标题',
    `dept_code`   VARCHAR(32)     NOT NULL COMMENT 'ROAD/LIGHTING/SANITATION/FACILITY',
    `dept_name`   VARCHAR(64)     NOT NULL,
    `work_zone`   VARCHAR(64)     NULL COMMENT '工作片区',
    `headcount`   INT             NOT NULL DEFAULT 1 COMMENT '招募人数',
    `requirement` VARCHAR(500)    NULL COMMENT '岗位要求',
    `description` VARCHAR(1000)   NULL COMMENT '岗位说明',
    `status`      VARCHAR(16)     NOT NULL DEFAULT 'OPEN' COMMENT 'OPEN/CLOSED',
    `deadline`    DATE            NULL COMMENT '截止日期',
    `cover_url`   VARCHAR(512)    NULL COMMENT '封面图',
    `create_by`   BIGINT UNSIGNED NULL,
    `update_by`   BIGINT UNSIGNED NULL,
    `create_time` DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP,
    `update_time` DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    `deleted`     TINYINT         NOT NULL DEFAULT 0,
    `version`     INT             NOT NULL DEFAULT 0,
    PRIMARY KEY (`id`),
    KEY `idx_recruitment_status` (`status`, `create_time`)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT ='工人招募公告';

CREATE TABLE IF NOT EXISTS `t_recruitment_apply`
(
    `id`              BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
    `recruitment_id`  BIGINT UNSIGNED NOT NULL,
    `user_id`         BIGINT UNSIGNED NOT NULL COMMENT '报名居民',
    `note`            VARCHAR(255)    NULL COMMENT '报名留言',
    `status`          VARCHAR(16)     NOT NULL DEFAULT 'PENDING' COMMENT 'PENDING/ACCEPTED/REJECTED',
    `create_by`       BIGINT UNSIGNED NULL,
    `update_by`       BIGINT UNSIGNED NULL,
    `create_time`     DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP,
    `update_time`     DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    `deleted`         TINYINT         NOT NULL DEFAULT 0,
    `version`         INT             NOT NULL DEFAULT 0,
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_recruit_apply` (`recruitment_id`, `user_id`),
    KEY `idx_recruit_apply_user` (`user_id`, `create_time`)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT ='招募报名记录';

USE city_event;
ALTER TABLE `t_event`
    ADD COLUMN `ai_review_priority` VARCHAR(16) NULL COMMENT 'AI复评紧急程度' AFTER `ai_suggestion`,
    ADD COLUMN `ai_review_reason` VARCHAR(300) NULL COMMENT 'AI复评理由' AFTER `ai_review_priority`,
    ADD COLUMN `ai_review_impact` VARCHAR(300) NULL COMMENT 'AI复评影响范围' AFTER `ai_review_reason`,
    ADD COLUMN `ai_review_response_minutes` INT NULL COMMENT 'AI建议响应时限（分钟）' AFTER `ai_review_impact`,
    ADD COLUMN `ai_review_confidence` DECIMAL(4, 3) NULL COMMENT 'AI复评置信度' AFTER `ai_review_response_minutes`,
    ADD COLUMN `ai_review_time` DATETIME NULL COMMENT 'AI复评时间' AFTER `ai_review_confidence`;

USE city_workorder;
ALTER TABLE `t_work_order`
    ADD COLUMN `leader_id` BIGINT UNSIGNED NULL COMMENT '责任组长' AFTER `assignee_name`,
    ADD COLUMN `leader_name` VARCHAR(64) NULL COMMENT '组长姓名快照' AFTER `leader_id`,
    ADD COLUMN `leader_review_time` DATETIME NULL COMMENT '组长复核时间' AFTER `submit_time`,
    ADD COLUMN `leader_review_remark` VARCHAR(255) NULL COMMENT '组长复核意见' AFTER `leader_review_time`,
    ADD KEY `idx_wo_leader_status` (`leader_id`, `status`);

CREATE TABLE IF NOT EXISTS `t_work_rating`
(
    `id`            BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
    `work_order_id` BIGINT UNSIGNED NOT NULL,
    `work_order_no` VARCHAR(32)     NOT NULL,
    `worker_id`     BIGINT UNSIGNED NOT NULL COMMENT '被评价处理人',
    `reporter_id`   BIGINT UNSIGNED NOT NULL COMMENT '评价居民',
    `rating`        TINYINT         NOT NULL COMMENT '1-5 星',
    `tags`          VARCHAR(255)    NULL COMMENT '标签，逗号分隔',
    `comment`       VARCHAR(255)    NULL COMMENT '文字评价',
    `create_time`   DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP,
    `update_time`   DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    `create_by`     BIGINT UNSIGNED NULL,
    `update_by`     BIGINT UNSIGNED NULL,
    `deleted`       TINYINT         NOT NULL DEFAULT 0,
    `version`       INT             NOT NULL DEFAULT 0,
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_work_rating_order` (`work_order_id`),
    KEY `idx_work_rating_worker` (`worker_id`, `create_time`)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT ='居民对工单处理的评分';
