SET NAMES utf8mb4;

USE city_event;

CREATE TABLE IF NOT EXISTS `t_event`
(
    `id`               BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '事件ID',
    `event_no`         VARCHAR(32)     NOT NULL COMMENT '事件编号',
    `reporter_id`      BIGINT UNSIGNED NOT NULL COMMENT '上报人ID',
    `category_code`    VARCHAR(32)     NOT NULL COMMENT '最终类别',
    `ai_category_code` VARCHAR(32)     NULL COMMENT 'AI建议类别',
    `title`            VARCHAR(100)    NOT NULL COMMENT '标题',
    `description`      VARCHAR(1000)   NOT NULL COMMENT '描述',
    `summary`          VARCHAR(255)    NULL COMMENT 'AI摘要',
    `priority`         VARCHAR(16)     NOT NULL DEFAULT 'MEDIUM' COMMENT '最终优先级',
    `ai_priority`      VARCHAR(16)     NULL COMMENT 'AI建议优先级',
    `ai_confidence`    DECIMAL(4, 3)   NULL COMMENT 'AI置信度',
    `ai_suggestion`    VARCHAR(600)    NULL COMMENT 'AI建议措施',
    `status`           VARCHAR(32)     NOT NULL COMMENT '状态',
    `province`         VARCHAR(64)     NULL,
    `city`             VARCHAR(64)     NULL,
    `district`         VARCHAR(64)     NULL,
    `street`           VARCHAR(128)    NULL,
    `area_code`        VARCHAR(32)     NULL COMMENT '区域编码',
    `address`          VARCHAR(255)    NOT NULL COMMENT '详细地址',
    `longitude`        DECIMAL(10, 6)  NOT NULL COMMENT '经度GCJ-02',
    `latitude`         DECIMAL(10, 6)  NOT NULL COMMENT '纬度GCJ-02',
    `photo_count`      INT             NOT NULL DEFAULT 0,
    `auditor_id`       BIGINT UNSIGNED NULL,
    `audit_time`       DATETIME        NULL,
    `reject_reason`    VARCHAR(255)    NULL,
    `dispatch_time`    DATETIME        NULL,
    `close_time`       DATETIME        NULL,
    `create_time`      DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP,
    `update_time`      DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    `create_by`        BIGINT UNSIGNED NULL,
    `update_by`        BIGINT UNSIGNED NULL,
    `deleted`          TINYINT         NOT NULL DEFAULT 0,
    `version`          INT             NOT NULL DEFAULT 0,
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_event_event_no` (`event_no`),
    KEY `idx_event_reporter` (`reporter_id`, `create_time`),
    KEY `idx_event_status_update` (`status`, `update_time`),
    KEY `idx_event_category` (`category_code`),
    KEY `idx_event_area` (`area_code`),
    KEY `idx_event_geo` (`longitude`, `latitude`)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT ='城市事件';

CREATE TABLE IF NOT EXISTS `t_attachment`
(
    `id`            BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '附件ID',
    `biz_type`      VARCHAR(32)     NOT NULL COMMENT 'EVENT_REPORT/WORKORDER_RESULT/WORKORDER_PROCESS',
    `biz_id`        BIGINT UNSIGNED NULL COMMENT '关联业务ID',
    `owner_id`      BIGINT UNSIGNED NOT NULL COMMENT '上传人',
    `original_name` VARCHAR(255)    NOT NULL COMMENT '原始文件名',
    `stored_name`   VARCHAR(128)    NOT NULL COMMENT '存储文件名',
    `file_path`     VARCHAR(512)    NOT NULL COMMENT '相对路径',
    `access_url`    VARCHAR(512)    NOT NULL COMMENT '访问URL',
    `content_type`  VARCHAR(64)     NOT NULL COMMENT 'MIME',
    `file_size`     INT UNSIGNED    NOT NULL COMMENT '字节',
    `sha256`        CHAR(64)        NOT NULL COMMENT '摘要',
    `status`        VARCHAR(16)     NOT NULL DEFAULT 'TEMP' COMMENT 'TEMP/BOUND',
    `create_time`   DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP,
    `update_time`   DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    `deleted`       TINYINT         NOT NULL DEFAULT 0,
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_attachment_stored` (`stored_name`),
    KEY `idx_attachment_biz` (`biz_type`, `biz_id`),
    KEY `idx_attachment_owner` (`owner_id`),
    KEY `idx_attachment_status` (`status`, `create_time`)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT ='附件元数据';

CREATE TABLE IF NOT EXISTS `t_event_status_log`
(
    `id`            BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
    `event_id`      BIGINT UNSIGNED NOT NULL,
    `event_no`      VARCHAR(32)     NOT NULL,
    `from_status`   VARCHAR(32)     NULL,
    `to_status`     VARCHAR(32)     NOT NULL,
    `action`        VARCHAR(32)     NOT NULL,
    `operator_id`   BIGINT UNSIGNED NOT NULL DEFAULT 0,
    `operator_role` VARCHAR(32)     NULL,
    `remark`        VARCHAR(255)    NULL,
    `create_time`   DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (`id`),
    KEY `idx_event_status_log_event` (`event_id`, `create_time`)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT ='事件状态流水';

CREATE TABLE IF NOT EXISTS `t_event_audit_log`
(
    `id`               BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
    `event_id`         BIGINT UNSIGNED NOT NULL,
    `auditor_id`       BIGINT UNSIGNED NOT NULL,
    `decision`         VARCHAR(16)     NOT NULL COMMENT 'APPROVE/REJECT',
    `before_category`  VARCHAR(32)     NULL,
    `after_category`   VARCHAR(32)     NULL,
    `before_priority`  VARCHAR(16)     NULL,
    `after_priority`   VARCHAR(16)     NULL,
    `reason`           VARCHAR(255)    NULL,
    `create_time`      DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (`id`),
    KEY `idx_event_audit_event` (`event_id`, `create_time`)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT ='事件审核记录';

CREATE TABLE IF NOT EXISTS `t_event_ai_snapshot`
(
    `id`             BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
    `event_id`       BIGINT UNSIGNED NOT NULL,
    `ai_call_log_id` BIGINT UNSIGNED NULL,
    `category_code`  VARCHAR(32)     NULL,
    `priority`       VARCHAR(16)     NULL,
    `summary`        VARCHAR(255)    NULL,
    `suggestion`     VARCHAR(600)    NULL,
    `confidence`     DECIMAL(4, 3)   NULL,
    `model_name`     VARCHAR(64)     NULL,
    `accepted`       TINYINT         NOT NULL DEFAULT 0,
    `create_time`    DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (`id`),
    KEY `idx_event_ai_event` (`event_id`)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT ='事件AI建议快照';

CREATE TABLE IF NOT EXISTS `t_event_category`
(
    `id`          BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
    `code`        VARCHAR(32)     NOT NULL,
    `name`        VARCHAR(64)     NOT NULL,
    `description` VARCHAR(255)    NULL,
    `sort`        INT             NOT NULL DEFAULT 0,
    `enabled`     TINYINT         NOT NULL DEFAULT 1,
    `create_time` DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP,
    `update_time` DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_category_code` (`code`)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT ='事件类别字典';

INSERT INTO `t_event_category` (`code`, `name`, `description`, `sort`)
VALUES ('ROAD_FACILITY', '道路设施', '路面破损、井盖缺失、护栏损坏', 10),
       ('PUBLIC_LIGHTING', '公共照明', '路灯不亮、线路裸露、灯杆倾斜', 20),
       ('ENVIRONMENT_SANITATION', '环境卫生', '垃圾堆积、污水外溢、乱贴乱画', 30),
       ('PUBLIC_FACILITY', '公共设施', '座椅、健身器材、公厕设施损坏', 40),
       ('OTHER', '其他', '无法归类，必须人工确认', 99)
ON DUPLICATE KEY UPDATE `name` = VALUES(`name`), `description` = VALUES(`description`);
