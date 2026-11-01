USE city_workorder;

CREATE TABLE IF NOT EXISTS `t_work_order`
(
    `id`                BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '工单ID',
    `work_order_no`     VARCHAR(32)     NOT NULL COMMENT '工单编号',
    `event_id`          BIGINT UNSIGNED NOT NULL COMMENT '事件ID',
    `event_no`          VARCHAR(32)     NOT NULL COMMENT '事件编号',
    `category_code`     VARCHAR(32)     NOT NULL COMMENT '事件类别快照',
    `priority`          VARCHAR(16)     NOT NULL COMMENT '优先级快照',
    `assignee_id`       BIGINT UNSIGNED NOT NULL COMMENT '处理人ID',
    `assignee_name`     VARCHAR(64)     NOT NULL COMMENT '处理人姓名快照',
    `dept_code`         VARCHAR(32)     NULL COMMENT '责任部门',
    `status`            VARCHAR(32)     NOT NULL COMMENT '工单状态',
    `creator_id`        BIGINT UNSIGNED NOT NULL COMMENT '派单人',
    `assign_time`       DATETIME        NOT NULL COMMENT '派单时间',
    `accept_time`       DATETIME        NULL COMMENT '接单时间',
    `submit_time`       DATETIME        NULL COMMENT '提交结果时间',
    `finish_time`       DATETIME        NULL COMMENT '完成时间',
    `acceptance_remark` VARCHAR(255)    NULL COMMENT '验收意见',
    `return_count`      INT             NOT NULL DEFAULT 0 COMMENT '退回次数',
    `reassign_count`    INT             NOT NULL DEFAULT 0 COMMENT '改派次数',
    `create_time`       DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP,
    `update_time`       DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    `create_by`         BIGINT UNSIGNED NULL,
    `update_by`         BIGINT UNSIGNED NULL,
    `deleted`           TINYINT         NOT NULL DEFAULT 0,
    `version`           INT             NOT NULL DEFAULT 0,
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_wo_no` (`work_order_no`),
    UNIQUE KEY `uk_wo_event_id` (`event_id`),
    KEY `idx_wo_assignee_status` (`assignee_id`, `status`),
    KEY `idx_wo_status_update` (`status`, `update_time`)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT ='处理工单';

CREATE TABLE IF NOT EXISTS `t_work_order_log`
(
    `id`             BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
    `work_order_id`  BIGINT UNSIGNED NOT NULL,
    `work_order_no`  VARCHAR(32)     NOT NULL,
    `action`         VARCHAR(32)     NOT NULL COMMENT 'CREATE/ACCEPT/PROCESS/SUBMIT/ACCEPT_PASS/ACCEPT_REJECT/REASSIGN',
    `from_status`    VARCHAR(32)     NULL,
    `to_status`      VARCHAR(32)     NULL,
    `operator_id`    BIGINT UNSIGNED NOT NULL,
    `operator_role`  VARCHAR(32)     NULL,
    `content`        VARCHAR(1000)   NULL,
    `attachment_ids` VARCHAR(512)    NULL COMMENT '附件ID列表，逗号分隔',
    `create_time`    DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (`id`),
    KEY `idx_wo_log_wo` (`work_order_id`, `create_time`)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT ='工单处理记录';

CREATE TABLE IF NOT EXISTS `t_work_order_acceptance`
(
    `id`            BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
    `work_order_id` BIGINT UNSIGNED NOT NULL,
    `work_order_no` VARCHAR(32)     NOT NULL,
    `acceptor_id`   BIGINT UNSIGNED NOT NULL,
    `result`        VARCHAR(16)     NOT NULL COMMENT 'PASS/REJECT',
    `remark`        VARCHAR(255)    NULL,
    `create_time`   DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (`id`),
    KEY `idx_wo_acc_wo` (`work_order_id`, `create_time`)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT ='工单验收记录';

CREATE TABLE IF NOT EXISTS `t_work_order_reassign`
(
    `id`              BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
    `work_order_id`   BIGINT UNSIGNED NOT NULL,
    `from_assignee_id` BIGINT UNSIGNED NOT NULL,
    `to_assignee_id`  BIGINT UNSIGNED NOT NULL,
    `reason`          VARCHAR(255)    NOT NULL,
    `operator_id`     BIGINT UNSIGNED NOT NULL,
    `create_time`     DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (`id`),
    KEY `idx_wo_reassign_wo` (`work_order_id`, `create_time`)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT ='工单改派记录';

-- Seata AT 模式回滚日志（官方脚本结构）
CREATE TABLE IF NOT EXISTS `undo_log`
(
    `branch_id`     BIGINT       NOT NULL COMMENT 'branch transaction id',
    `xid`           VARCHAR(128) NOT NULL COMMENT 'global transaction id',
    `context`       VARCHAR(128) NOT NULL COMMENT 'undo_log context,such as serialization',
    `rollback_info` LONGBLOB     NOT NULL COMMENT 'rollback info',
    `log_status`    INT(11)      NOT NULL COMMENT '0:normal status,1:defense status',
    `log_created`   DATETIME(6)  NOT NULL COMMENT 'create datetime',
    `log_modified`  DATETIME(6)  NOT NULL COMMENT 'modify datetime',
    UNIQUE KEY `ux_undo_log` (`xid`, `branch_id`)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT ='AT transaction mode undo table';
